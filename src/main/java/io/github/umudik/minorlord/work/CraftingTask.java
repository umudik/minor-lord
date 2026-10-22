package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.production.*;
import io.github.umudik.minorlord.roles.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;

/** Live native crafting recipes, physical exact ingredient collection, station visit and product delivery. */
public final class CraftingTask implements WorkSession.Task<Villager> {
    private final GlobalPos home;private final Role role;private final TaskWalkTarget walk=new TaskWalkTarget();
    private CraftingRecipe recipe;private CraftingInput input;private BlockPos station;
    private Map<ItemVariant,Long> costs=Map.of();private Map<ItemVariant,Long> pending=new LinkedHashMap<>();
    /** Planning walks every crafting recipe; while the village stock keeps changing it is redone at most this often. */
    private static final long REPLAN_TICKS=100;
    private long batches,checked=-1,plannedAt=-REPLAN_TICKS;
    private Map<ItemVariant,Long> plannedOutputs=Map.of();
    private final Map<Set<?>,InventoryTransfers.Endpoint<ItemVariant>> targetCache=new HashMap<>();
    public CraftingTask(GlobalPos home,Role role){this.home=home;this.role=role;}
    public boolean active(){return recipe!=null||!pending.isEmpty();}
    private Map<ItemVariant,Long> contents(InventoryTransfers.Endpoint<ItemVariant> inventory){
        Map<ItemVariant,Long> result=new LinkedHashMap<>();for(var view:inventory.storage())
            if(!view.isResourceBlank()&&view.getAmount()>0)result.merge(view.getResource(),view.getAmount(),Math::addExact);
        return result;
    }
    private Map<ItemVariant,Long> available(ServerLevel level){
        Map<ItemVariant,Long> result=new LinkedHashMap<>();Set<Object> seen=new HashSet<>();
        for(var p:VillageInventories.chests(level,home)){
            var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty()||!Collections.disjoint(seen,endpoint.get().inventoryIdentities()))continue;
            seen.addAll(endpoint.get().inventoryIdentities());
            for(var e:contents(endpoint.get()).entrySet())if(VillageInventories.matches(level,p,e.getKey()))result.merge(e.getKey(),e.getValue(),Math::addExact);
        }
        return result;
    }
    private Optional<InventoryTransfers.Endpoint<ItemVariant>> target(Villager v,ItemVariant item){
        return VillageInventories.nearest(v,home,item,plannedOutputs.getOrDefault(item,1L))
            .flatMap(p->LiveChestAccess.resolve((ServerLevel)v.level(),p))
            .map(endpoint->targetCache.computeIfAbsent(endpoint.inventoryIdentities(),key->endpoint));
    }
    private Optional<BlockPos> workstation(Villager v){
        return WorkResources.station(v,home,s->s.is(Blocks.CRAFTING_TABLE));
    }
    private boolean plan(Villager v){
        var level=(ServerLevel)v.level();station=workstation(v).orElse(null);if(station==null)return false;
        var supply=available(level);
        // Among everything the stock allows, the best gear for its need is made first (diamond before iron before copper).
        CraftingRecipe bestRecipe=null;CraftingInput bestGrid=null;Map<ItemVariant,Long> bestCosts=null,bestOutputs=null;long bestBatches=0;double bestValue=-1;
        for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),RecipeType.CRAFTING)){
            if(!(holder.value() instanceof CraftingRecipe candidate)||candidate.placementInfo().isImpossibleToPlace())continue;
            BatchAllocator.Allocation<ItemVariant> allocation;
            try{allocation=RuntimeRecipes.planIngredients(candidate,supply,Long.MAX_VALUE);}catch(IllegalArgumentException unsupported){continue;}
            if(allocation.batches()==0)continue;
            var unit=RuntimeRecipes.planIngredients(candidate,supply,1);
            var placement=candidate.placementInfo();var slots=placement.slotsToIngredientIndex();
            int width=candidate instanceof ShapedRecipe shaped?shaped.getWidth():3;
            int height=candidate instanceof ShapedRecipe shaped?shaped.getHeight():3;
            var stacks=new ArrayList<ItemStack>(Collections.nCopies(width*height,ItemStack.EMPTY));
            if(slots.size()>stacks.size())continue;
            for(int slot=0;slot<slots.size();slot++){
                int ingredient=slots.getInt(slot);if(ingredient<0)continue;
                var variant=unit.perIngredient().get(ingredient).keySet().iterator().next();stacks.set(slot,variant.toStack());
            }
            var grid=CraftingInput.of(width,height,stacks);if(!candidate.matches(grid,level))continue;
            var output=candidate.assemble(grid);
            if(output.isEmpty()||!CraftingRoles.accepts(role,candidate,output))continue;
            // Gear: as many as the village's users still lack. Anything else: the old per-product chest limit.
            var deficit=io.github.umudik.minorlord.production.Demand.deficit(level,home,output);
            long allowed=allocation.batches();
            if(deficit.isPresent()){
                if(deficit.getAsLong()<=0)continue;
                allowed=Math.min(allowed,(deficit.getAsLong()+output.getCount()-1)/output.getCount());
            }else{
                // Anything else: only up to the per-product limit in total, never as much as the stock allows.
                var room=StockLimits.room(role,WorkResources.stock(level,home,ItemVariant.of(output)),output.getMaxStackSize());
                if(room.isPresent()){if(room.getAsLong()<=0)continue;allowed=Math.min(allowed,(room.getAsLong()+output.getCount()-1)/output.getCount());}
            }
            double value=io.github.umudik.minorlord.production.Demand.value(level.registryAccess(),output);
            if(value<=bestValue)continue;
            var outputs=new LinkedHashMap<ItemVariant,Long>(RecipeRemainders.inspect(candidate,grid));outputs.merge(ItemVariant.of(output),(long)output.getCount(),Math::addExact);
            plannedOutputs=outputs;targetCache.clear();
            if(OutputPreflight.check(outputs,item->target(v,item))!=OutputPreflight.Result.READY)continue;
            var needed=new LinkedHashMap<ItemVariant,Long>();for(var stack:grid.items())if(!stack.isEmpty())needed.merge(ItemVariant.of(stack),1L,Math::addExact);
            bestRecipe=candidate;bestGrid=grid;bestCosts=needed;bestOutputs=outputs;bestBatches=allowed;bestValue=value;
        }
        if(bestRecipe==null)return false;
        // One complete native grid at a time; keep this recipe active and repeat while it is producible and needed.
        plannedOutputs=bestOutputs;targetCache.clear();recipe=bestRecipe;input=bestGrid;costs=bestCosts;batches=bestBatches;return true;
    }
    @Override public WorkSession.Step step(Villager v){
        targetCache.clear();
        var level=(ServerLevel)v.level();var carry=VillageInventories.carried(v);
        if(!pending.isEmpty()){
            for(var e:List.copyOf(pending.entrySet())){
                if(!Delivery.deliverable(v,home,e.getKey(),e.getValue()))continue;
                if(Delivery.fallback(v,home,e.getKey(),e.getValue(),walk))pending.remove(e.getKey());
                return WorkSession.Step.CONTINUE;
            }
            pending.clear(); // Nothing left with a destination: the stacks stay carried and are retried below.
        }
        if(recipe==null){
            var carried=contents(carry);if(!carried.isEmpty()){
                for(var e:carried.entrySet()){
                    long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
                    if(!Delivery.deliverable(v,home,e.getKey(),amount))continue;
                    Delivery.fallback(v,home,e.getKey(),amount,walk);return WorkSession.Step.CONTINUE;
                }
            }
            if(checked==VillageInventories.revision(home)||level.getGameTime()-plannedAt<REPLAN_TICKS)return WorkSession.Step.IDLE;
            checked=VillageInventories.revision(home);plannedAt=level.getGameTime();if(!plan(v))return WorkSession.Step.IDLE;
        }
        var held=contents(carry);var supply=available(level);
        for(var e:costs.entrySet())if(held.getOrDefault(e.getKey(),0L)+supply.getOrDefault(e.getKey(),0L)<e.getValue()){
            recipe=null;return WorkSession.Step.CONTINUE;
        }
        for(var e:costs.entrySet()){
            long missing=e.getValue()-held.getOrDefault(e.getKey(),0L);if(missing<=0)continue;
            var candidates=VillageInventories.chests(level,home).stream().filter(p->VillageInventories.matches(level,p,e.getKey()))
                .sorted(Comparator.comparingDouble(p->p.distToCenterSqr(v.position()))).toList();
            for(var p:candidates){
                var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty())continue;
                long count=Math.min(missing,contents(endpoint.get()).getOrDefault(e.getKey(),0L));if(count==0)continue;
                if(!Delivery.arrived(v,p)){walk.offer(v,p,0.5f,1);return WorkSession.Step.CONTINUE;}
                InventoryTransfers.moveExact(endpoint.get(),carry,e.getKey(),count,()->Delivery.arrived(v,p)&&VillageInventories.matches(level,p,e.getKey()));
                walk.release(v);return WorkSession.Step.CONTINUE;
            }
            recipe=null;return WorkSession.Step.CONTINUE;
        }
        if(!level.hasChunkAt(station)||!level.getBlockState(station).is(Blocks.CRAFTING_TABLE)){recipe=null;return WorkSession.Step.CONTINUE;}
        if(!Delivery.arrived(v,station)){walk.offer(v,station,0.5f,1);return WorkSession.Step.CONTINUE;}
        if(CarriedCrafting.craft(recipe,input,level,carry,item->target(v,item),()->Delivery.arrived(v,station)&&level.getBlockState(station).is(Blocks.CRAFTING_TABLE))){
            pending=new LinkedHashMap<>(RecipeRemainders.inspect(recipe,input));var result=recipe.assemble(input);
            pending.merge(ItemVariant.of(result),(long)result.getCount(),Math::addExact);batches--;
            if(batches==0)recipe=null;
        }
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
