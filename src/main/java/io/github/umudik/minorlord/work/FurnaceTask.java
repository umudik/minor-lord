package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.production.*;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Native furnace/smoker/blast-furnace processing; persistent job reference, no fake ticks or free fuel. */
public final class FurnaceTask implements WorkSession.Task<Villager>{
    public record Job(GlobalPos machine,ItemStack input,ItemStack output){
        public static final Codec<Job> CODEC=RecordCodecBuilder.create(i->i.group(
            GlobalPos.CODEC.fieldOf("machine").forGetter(Job::machine),ItemStack.CODEC.fieldOf("input").forGetter(Job::input),
            ItemStack.CODEC.fieldOf("output").forGetter(Job::output)).apply(i,Job::new));
    }
    private static final AttachmentType<Job> JOB=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","furnace_job"),Job.CODEC);
    private static final Map<GlobalPos,UUID> OWNERS=new HashMap<>();
    private final GlobalPos home;private final Role role;private final TaskWalkTarget walk=new TaskWalkTarget();
    public FurnaceTask(GlobalPos home,Role role){this.home=home;this.role=role;}
    public static void clear(){OWNERS.clear();}
    public static void initialize(){}
    public static void release(Villager v){OWNERS.entrySet().removeIf(e->e.getValue().equals(v.getUUID()));((AttachmentTarget)v).removeAttached(JOB);}
    public boolean active(Villager v){return ((AttachmentTarget)v).hasAttached(JOB);}
    private boolean accepts(ItemStack result){var traits=OutputTraits.inspect(result);return role==Role.COOK?traits.food():traits.ingot();}
    private boolean fuelTargets(Villager v,ItemStack output,ItemStack fuel){
        Map<ItemVariant,Long> products=new LinkedHashMap<>();products.put(ItemVariant.of(output),(long)output.getCount());
        var remainder=fuel.getItem().getCraftingRemainder();if(remainder!=null){var stack=remainder.create();products.merge(ItemVariant.of(stack),(long)stack.getCount(),Long::sum);}
        return WorkResources.targets(v,home,products);
    }
    private InventoryTransfers.Endpoint<ItemVariant> slot(AbstractFurnaceBlockEntity machine,ServerLevel level,int slot){
        return new InventoryTransfers.Endpoint<>(Set.of(GlobalPos.of(level.dimension(),machine.getBlockPos())),ContainerStorage.of(machine,null).getSlot(slot));
    }
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var attachments=(AttachmentTarget)v;var job=attachments.getAttached(JOB);
        if(job==null){
            if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
            var candidate=WorkResources.station(v,home,state->state.getBlock() instanceof AbstractFurnaceBlock).orElse(null);
            if(candidate==null||!(level.getBlockEntity(candidate) instanceof AbstractFurnaceBlockEntity machine))return WorkSession.Step.CONTINUE;
            var key=GlobalPos.of(level.dimension(),candidate);
            if(OWNERS.containsKey(key)||!machine.getItem(0).isEmpty()||!machine.getItem(2).isEmpty())return WorkSession.Step.CONTINUE;
            RecipeType<?> type=level.getBlockState(candidate).is(Blocks.BLAST_FURNACE)?RecipeType.BLASTING:
                level.getBlockState(candidate).is(Blocks.SMOKER)?RecipeType.SMOKING:RecipeType.SMELTING;
            outer:for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),type)){
                if(!(holder.value() instanceof AbstractCookingRecipe recipe))continue;
                for(var e:WorkResources.available(level,home).entrySet()){
                    var input=new SingleRecipeInput(e.getKey().toStack());if(!recipe.matches(input,level))continue;
                    var output=recipe.assemble(input);if(output.isEmpty()||!accepts(output)||VillageInventories.nearest(v,home,ItemVariant.of(output),output.getCount()).isEmpty())continue;
                    boolean hasFuel=level.getBlockState(candidate).getValue(AbstractFurnaceBlock.LIT)||machine.getItem(1).has(DataComponents.COOKING_FUEL);
                    if(!hasFuel && WorkResources.available(level,home).keySet().stream().noneMatch(f->f.toStack().has(DataComponents.COOKING_FUEL)
                            && fuelTargets(v,output,f.toStack())&&machine.canPlaceItem(1,f.toStack())))continue;
                    job=new Job(key,e.getKey().toStack(),output.copy());attachments.setAttached(JOB,job);OWNERS.put(key,v.getUUID());break outer;
                }
            }if(job==null)return WorkSession.Step.CONTINUE;
        }
        // The claim stayed behind with the old dimension's entity; the machine keeps its real items.
        if(!job.machine.dimension().equals(level.dimension())){attachments.removeAttached(JOB);return WorkSession.Step.CONTINUE;}
        if(!level.hasChunkAt(job.machine.pos()))return WorkSession.Step.CONTINUE;
        if(!(level.getBlockEntity(job.machine.pos()) instanceof AbstractFurnaceBlockEntity machine)){
            OWNERS.remove(job.machine);attachments.removeAttached(JOB);return WorkSession.Step.CONTINUE;
        }
        if(OWNERS.putIfAbsent(job.machine,v.getUUID())!=null&&!v.getUUID().equals(OWNERS.get(job.machine)))return WorkSession.Step.CONTINUE;
        var carry=VillageInventories.carried(v);
        if(!machine.getItem(2).isEmpty()){
            var produced=machine.getItem(2);if(!ItemStack.isSameItemSameComponents(produced,job.output))return WorkSession.Step.CONTINUE;
            var item=ItemVariant.of(produced);long amount=produced.getCount();
            if(VillageInventories.nearest(v,home,item,amount).isEmpty())return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            try(var tx=Transaction.openOuter()){
                if(slot(machine,level,2).storage().extract(item,amount,tx)!=amount||carry.storage().insert(item,amount,tx)!=amount)return WorkSession.Step.CONTINUE;
                var remainder=machine.getItem(1);
                if(!remainder.isEmpty()&&!remainder.has(DataComponents.COOKING_FUEL)){
                    var returned=ItemVariant.of(remainder);long count=remainder.getCount();
                    if(VillageInventories.nearest(v,home,returned,count).isEmpty()
                        ||slot(machine,level,1).storage().extract(returned,count,tx)!=count||carry.storage().insert(returned,count,tx)!=count)return WorkSession.Step.CONTINUE;
                }
                tx.commit();
                OWNERS.remove(job.machine);attachments.removeAttached(JOB);
            }return WorkSession.Step.CONTINUE;
        }
        if(!machine.getItem(0).isEmpty())return WorkSession.Step.CONTINUE; // Let the native machine cook.
        boolean fueled=level.getBlockState(job.machine.pos()).getValue(AbstractFurnaceBlock.LIT)||machine.getItem(1).has(DataComponents.COOKING_FUEL);
        if(!fueled){
            var expectedOutput=job.output;
            var fuel=WorkResources.available(level,home).keySet().stream().filter(f->f.toStack().has(DataComponents.COOKING_FUEL)
                &&fuelTargets(v,expectedOutput,f.toStack())&&machine.canPlaceItem(1,f.toStack())).findFirst();
            // A previously collected fuel item is also eligible if its source changed afterward.
            if(fuel.isEmpty())fuel=WorkResources.contents(carry).keySet().stream().filter(f->f.toStack().has(DataComponents.COOKING_FUEL)&&fuelTargets(v,expectedOutput,f.toStack())&&machine.canPlaceItem(1,f.toStack())).findFirst();
            if(fuel.isEmpty()||!WorkResources.gather(v,home,fuel.get(),1,walk))return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            InventoryTransfers.moveExact(carry,slot(machine,level,1),fuel.get(),1,()->Delivery.arrived(v,machine.getBlockPos()));return WorkSession.Step.CONTINUE;
        }
        var ingredient=ItemVariant.of(job.input);if(!WorkResources.gather(v,home,ingredient,1,walk))return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
        if(VillageInventories.nearest(v,home,ItemVariant.of(job.output),job.output.getCount()).isEmpty())return WorkSession.Step.CONTINUE;
        InventoryTransfers.moveExact(carry,slot(machine,level,0),ingredient,1,()->Delivery.arrived(v,machine.getBlockPos())
            &&level.getBlockEntity(machine.getBlockPos())==machine);
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
