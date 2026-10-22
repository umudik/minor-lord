package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.mixin.BrewingStandAccess;
import io.github.umudik.minorlord.production.RuntimeRecipes;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** Owned native brewing stand; native timer, real potion/reagent/fuel, persistent job and leftovers. */
public final class BrewingTask implements WorkSession.Task<Villager>{
    public record Job(GlobalPos machine,ItemStack input,ItemStack reagent,ItemStack output){
        public static final Codec<Job> CODEC=RecordCodecBuilder.create(i->i.group(
            GlobalPos.CODEC.fieldOf("machine").forGetter(Job::machine),ItemStack.CODEC.fieldOf("input").forGetter(Job::input),
            ItemStack.CODEC.fieldOf("reagent").forGetter(Job::reagent),ItemStack.CODEC.fieldOf("output").forGetter(Job::output)).apply(i,Job::new));
    }
    private static final AttachmentType<Job> JOB=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","brewing_job"),Job.CODEC);
    private static final Map<GlobalPos,UUID> OWNERS=new HashMap<>();
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();
    public BrewingTask(GlobalPos home){this.home=home;}
    public static void initialize(){}
    public static void clear(){OWNERS.clear();}
    public static void release(Villager v){OWNERS.entrySet().removeIf(e->e.getValue().equals(v.getUUID()));((AttachmentTarget)v).removeAttached(JOB);}
    private InventoryTransfers.Endpoint<ItemVariant> slot(BrewingStandBlockEntity m,ServerLevel l,int slot){
        return new InventoryTransfers.Endpoint<>(Set.of(GlobalPos.of(l.dimension(),m.getBlockPos())),ContainerStorage.of(m,null).getSlot(slot));
    }
    private Map<ItemVariant,Long> outputs(Job job){
        Map<ItemVariant,Long> result=new LinkedHashMap<>();result.put(ItemVariant.of(job.output),(long)job.output.getCount());
        var remainder=job.reagent.getItem().getCraftingRemainder();if(remainder!=null){var stack=remainder.create();result.merge(ItemVariant.of(stack),(long)stack.getCount(),Long::sum);}
        return result;
    }
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var attached=(AttachmentTarget)v;var job=attached.getAttached(JOB);
        if(job==null){
            if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
            var p=WorkResources.station(v,home,s->s.is(Blocks.BREWING_STAND)).orElse(null);
            if(p==null||!(level.getBlockEntity(p) instanceof BrewingStandBlockEntity machine))return WorkSession.Step.CONTINUE;
            var key=GlobalPos.of(level.dimension(),p);var access=(BrewingStandAccess)machine;
            if(OWNERS.containsKey(key)||access.minorLordBrewTime()>0)return WorkSession.Step.CONTINUE;
            for(int slot=0;slot<4;slot++)if(!machine.getItem(slot).isEmpty())return WorkSession.Step.CONTINUE;
            var available=WorkResources.available(level,home);
            if(access.minorLordFuel()==0&&!machine.getItem(4).has(DataComponents.BREWING_FUEL)
                &&available.keySet().stream().noneMatch(f->f.toStack().has(DataComponents.BREWING_FUEL)))return WorkSession.Step.CONTINUE;
            outer:for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),RecipeType.BREWING)){
                if(!(holder.value() instanceof BrewingRecipe recipe))continue;
                for(var input:available.keySet())if(recipe.getInput().test(input.toStack()))
                    for(var reagent:available.keySet())if(recipe.getReagent().test(reagent.toStack())){
                        var proposed=new BrewingInput(input.toStack(),reagent.toStack());if(!recipe.matches(proposed,level))continue;
                        var output=recipe.assemble(proposed);if(output.isEmpty())continue;
                        Map<ItemVariant,Long> required=new HashMap<>();required.merge(input,1L,Long::sum);required.merge(reagent,1L,Long::sum);
                        if(required.entrySet().stream().anyMatch(e->available.getOrDefault(e.getKey(),0L)<e.getValue()))continue;
                        if(access.minorLordFuel()==0&&!machine.getItem(4).has(DataComponents.BREWING_FUEL)
                            &&available.keySet().stream().noneMatch(f->f.toStack().has(DataComponents.BREWING_FUEL)
                                &&available.getOrDefault(f,0L)>=required.getOrDefault(f,0L)+1))continue;
                        var choice=new Job(key,input.toStack(),reagent.toStack(),output);
                        if(!WorkResources.targets(v,home,outputs(choice)))continue;
                        job=choice;attached.setAttached(JOB,job);OWNERS.put(key,v.getUUID());break outer;
                    }
            }if(job==null)return WorkSession.Step.IDLE;
        }
        // The claim stayed behind with the old dimension's entity; the machine keeps its real items.
        if(!job.machine.dimension().equals(level.dimension())){attached.removeAttached(JOB);return WorkSession.Step.CONTINUE;}
        if(!level.hasChunkAt(job.machine.pos()))return WorkSession.Step.CONTINUE;
        if(!(level.getBlockEntity(job.machine.pos()) instanceof BrewingStandBlockEntity machine)){
            OWNERS.remove(job.machine);attached.removeAttached(JOB);return WorkSession.Step.CONTINUE;
        }
        if(OWNERS.putIfAbsent(job.machine,v.getUUID())!=null&&!v.getUUID().equals(OWNERS.get(job.machine)))return WorkSession.Step.CONTINUE;
        var carry=VillageInventories.carried(v);var access=(BrewingStandAccess)machine;
        if(ItemStack.isSameItemSameComponents(machine.getItem(0),job.output)){
            if(!WorkResources.targets(v,home,outputs(job)))return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            try(var tx=Transaction.openOuter()){
                var output=ItemVariant.of(machine.getItem(0));long amount=machine.getItem(0).getCount();
                if(slot(machine,level,0).storage().extract(output,amount,tx)!=amount||carry.storage().insert(output,amount,tx)!=amount)return WorkSession.Step.CONTINUE;
                var leftover=machine.getItem(3);
                if(!leftover.isEmpty()){
                    var item=ItemVariant.of(leftover);long count=leftover.getCount();
                    if(slot(machine,level,3).storage().extract(item,count,tx)!=count||carry.storage().insert(item,count,tx)!=count)return WorkSession.Step.CONTINUE;
                }
                tx.commit();OWNERS.remove(job.machine);attached.removeAttached(JOB);
            }return WorkSession.Step.CONTINUE;
        }
        if(access.minorLordBrewTime()>0)return WorkSession.Step.CONTINUE;
        if(access.minorLordFuel()==0&&!machine.getItem(4).has(DataComponents.BREWING_FUEL)){
            var supply=new HashMap<>(WorkResources.available(level,home));
            WorkResources.contents(carry).forEach((k,count)->supply.merge(k,count,Long::sum));
            Map<ItemVariant,Long> reserved=new HashMap<>();
            if(machine.getItem(0).isEmpty())reserved.merge(ItemVariant.of(job.input),1L,Long::sum);
            if(machine.getItem(3).isEmpty())reserved.merge(ItemVariant.of(job.reagent),1L,Long::sum);
            var fuel=java.util.stream.Stream.concat(WorkResources.contents(carry).keySet().stream(),WorkResources.available(level,home).keySet().stream())
                .filter(f->f.toStack().has(DataComponents.BREWING_FUEL)&&supply.getOrDefault(f,0L)>=reserved.getOrDefault(f,0L)+1).findFirst();
            if(fuel.isEmpty()||!WorkResources.gather(v,home,fuel.get(),1,walk))return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            InventoryTransfers.moveExact(carry,slot(machine,level,4),fuel.get(),1,()->Delivery.arrived(v,machine.getBlockPos()));return WorkSession.Step.CONTINUE;
        }
        int targetSlot=machine.getItem(3).isEmpty()?3:0;
        if(targetSlot==0&&!machine.getItem(0).isEmpty())return WorkSession.Step.IDLE;
        var ingredient=ItemVariant.of(targetSlot==3?job.reagent:job.input);
        if(!WorkResources.gather(v,home,ingredient,1,walk))return WorkSession.Step.CONTINUE;
        if(!WorkResources.targets(v,home,outputs(job)))return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
        InventoryTransfers.moveExact(carry,slot(machine,level,targetSlot),ingredient,1,()->Delivery.arrived(v,machine.getBlockPos())&&level.getBlockEntity(machine.getBlockPos())==machine);
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
