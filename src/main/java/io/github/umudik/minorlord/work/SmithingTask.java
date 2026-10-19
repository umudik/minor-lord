package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.production.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;

/** Actual smithing inputs/templates are consumed at a smithing table; no free netherite upgrades. */
public final class SmithingTask implements WorkSession.Task<Villager>{
    private final GlobalPos home;private final Role role;private final TaskWalkTarget walk=new TaskWalkTarget();
    private SmithingRecipe recipe;private SmithingRecipeInput input;private Map<ItemVariant,Long> costs=Map.of();
    private BlockPos station;private long remaining,checked=-1;private boolean delivering;
    public SmithingTask(GlobalPos home,Role role){this.home=home;this.role=role;}
    public boolean active(){return recipe!=null||delivering;}
    private boolean accepts(ItemStack output){
        boolean tool=output.is(ItemTags.PICKAXES)||output.is(ItemTags.AXES)||output.is(ItemTags.HOES)||output.is(ItemTags.SHOVELS);
        var traits=OutputTraits.inspect(output);
        return role==Role.ARMORER?traits.armor():role==Role.TOOLSMITH?tool:traits.meleeWeapon()&&!tool;
    }
    private boolean plan(Villager v){
        var level=(ServerLevel)v.level();station=WorkResources.station(v,home,s->s.is(Blocks.SMITHING_TABLE)).orElse(null);if(station==null)return false;
        var available=WorkResources.available(level,home);
        for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),RecipeType.SMITHING)){
            if(!(holder.value() instanceof SmithingRecipe candidate))continue;
            var templates=candidate.templateIngredient().map(i->available.keySet().stream().filter(k->i.test(k.toStack())).map(ItemVariant::toStack).toList()).orElse(List.of(ItemStack.EMPTY));
            var bases=available.keySet().stream().filter(k->candidate.baseIngredient().test(k.toStack())).map(ItemVariant::toStack).toList();
            var additions=candidate.additionIngredient().map(i->available.keySet().stream().filter(k->i.test(k.toStack())).map(ItemVariant::toStack).toList()).orElse(List.of(ItemStack.EMPTY));
            for(var template:templates)for(var base:bases)for(var addition:additions){
                var proposed=new SmithingRecipeInput(template,base,addition);if(!candidate.matches(proposed,level))continue;
                var output=candidate.assemble(proposed);var variant=ItemVariant.of(output);
                if(output.isEmpty()||!accepts(output)||!StockLimits.shouldStart(role,WorkResources.stock(level,home,variant),output.getMaxStackSize())
                    ||VillageInventories.nearest(v,home,variant,output.getCount()).isEmpty())continue;
                Map<ItemVariant,Long> required=new LinkedHashMap<>();
                for(var stack:List.of(template,base,addition))if(!stack.isEmpty())required.merge(ItemVariant.of(stack),1L,Long::sum);
                long count=required.entrySet().stream().mapToLong(e->available.getOrDefault(e.getKey(),0L)/e.getValue()).min().orElse(0);
                if(count==0)continue;recipe=candidate;input=proposed;costs=required;remaining=count;return true;
            }
        }return false;
    }
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();
        if(delivering){if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;delivering=false;}
        if(recipe==null){
            if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
            if(checked==VillageInventories.revision(home))return WorkSession.Step.CONTINUE;checked=VillageInventories.revision(home);
            if(!plan(v))return WorkSession.Step.CONTINUE;
        }
        for(var e:costs.entrySet())if(!WorkResources.gather(v,home,e.getKey(),e.getValue(),walk)){
            if(WorkResources.available(level,home).getOrDefault(e.getKey(),0L)+WorkResources.contents(VillageInventories.carried(v)).getOrDefault(e.getKey(),0L)<e.getValue())recipe=null;
            return WorkSession.Step.CONTINUE;
        }
        if(!level.hasChunkAt(station)||!level.getBlockState(station).is(Blocks.SMITHING_TABLE)){recipe=null;return WorkSession.Step.CONTINUE;}
        if(!Delivery.arrived(v,station)){walk.offer(v,station,0.5f,1);return WorkSession.Step.CONTINUE;}
        if(!recipe.matches(input,level)){recipe=null;return WorkSession.Step.CONTINUE;}
        var output=recipe.assemble(input);var variant=ItemVariant.of(output);
        if(VillageInventories.nearest(v,home,variant,output.getCount()).isEmpty())return WorkSession.Step.CONTINUE;
        if(InventoryConversions.convertInPlace(VillageInventories.carried(v),costs,Map.of(variant,(long)output.getCount()),
            ()->Delivery.arrived(v,station)&&level.getBlockState(station).is(Blocks.SMITHING_TABLE))==InventoryConversions.Result.PRODUCED){
            delivering=true;if(--remaining==0)recipe=null;
        }return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
