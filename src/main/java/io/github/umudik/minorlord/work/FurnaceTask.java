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

/**
 * Native plain-furnace processing for Smelter and Cook (blast furnaces and smokers stay the armorer's and butcher's); persistent job reference, no fake ticks or free fuel. Inputs go in
 * batches, fuel is added whenever the machine burns out, finished products are taken out as they appear.
 */
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
    /** A new job is looked for at most this often while nothing can be smelted. */
    private static final long RETRY_TICKS=100;
    /** Inputs put into the machine at once; the native furnace works through them while fuel is added as it burns out. */
    private static final long BATCH=16;
    private long retryAt;
    private static RecipeType<? extends AbstractCookingRecipe> type(ServerLevel level,BlockPos machine){
        var state=level.getBlockState(machine);
        return state.is(Blocks.BLAST_FURNACE)?RecipeType.BLASTING:state.is(Blocks.SMOKER)?RecipeType.SMOKING:RecipeType.SMELTING;
    }
    private static ItemStack result(ServerLevel level,BlockPos machine,ItemStack input){
        var single=new SingleRecipeInput(input);
        for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),type(level,machine)))
            if(holder.value() instanceof AbstractCookingRecipe recipe&&recipe.matches(single,level))return recipe.assemble(single);
        return ItemStack.EMPTY;
    }
    private static boolean fueled(ServerLevel level,AbstractFurnaceBlockEntity machine){
        return level.getBlockState(machine.getBlockPos()).getValue(AbstractFurnaceBlock.LIT)||machine.getItem(1).has(DataComponents.COOKING_FUEL);
    }
    /**
     * Work for an unowned machine: an input left cooking in it is tended on; otherwise the first village input whose
     * product this role makes, has a chest and has fuel. A finished product this role does not make keeps it out.
     */
    private Job plan(Villager v,ServerLevel level,AbstractFurnaceBlockEntity machine,GlobalPos key){
        var produced=machine.getItem(2);if(!produced.isEmpty()&&!accepts(produced))return null;
        var cooking=machine.getItem(0);
        if(!cooking.isEmpty()){
            var output=result(level,machine.getBlockPos(),cooking);
            return output.isEmpty()||!accepts(output)?null:new Job(key,cooking.copyWithCount(1),output.copy());
        }
        var available=WorkResources.available(level,home);
        for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),type(level,machine.getBlockPos()))){
            if(!(holder.value() instanceof AbstractCookingRecipe recipe))continue;
            for(var e:available.entrySet()){
                var input=new SingleRecipeInput(e.getKey().toStack());if(!recipe.matches(input,level))continue;
                var output=recipe.assemble(input);if(output.isEmpty()||!accepts(output)||VillageInventories.nearest(v,home,ItemVariant.of(output),output.getCount()).isEmpty())continue;
                if(!fueled(level,machine)&&available.keySet().stream().noneMatch(f->f.toStack().has(DataComponents.COOKING_FUEL)
                        &&fuelTargets(v,output,f.toStack())&&machine.canPlaceItem(1,f.toStack())))continue;
                return new Job(key,e.getKey().toStack(),output.copy());
            }
        }
        return null;
    }
    private void end(Villager v,Job job){OWNERS.remove(job.machine);((AttachmentTarget)v).removeAttached(JOB);}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var attachments=(AttachmentTarget)v;var job=attachments.getAttached(JOB);
        if(job==null){
            if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
            if(level.getGameTime()<retryAt)return WorkSession.Step.IDLE;
            // Only plain furnaces: blast furnaces and smokers are armorer and butcher workstations.
            var candidate=WorkResources.station(v,home,state->state.is(Blocks.FURNACE),
                p->!OWNERS.containsKey(GlobalPos.of(level.dimension(),p))).orElse(null);
            if(candidate==null||!(level.getBlockEntity(candidate) instanceof AbstractFurnaceBlockEntity machine))return WorkSession.Step.IDLE;
            var key=GlobalPos.of(level.dimension(),candidate);
            job=plan(v,level,machine,key);
            if(job==null){retryAt=level.getGameTime()+RETRY_TICKS;return WorkSession.Step.IDLE;}
            attachments.setAttached(JOB,job);OWNERS.put(key,v.getUUID());
        }
        // The claim stayed behind with the old dimension's entity; the machine keeps its real items.
        if(!job.machine.dimension().equals(level.dimension())){attachments.removeAttached(JOB);return WorkSession.Step.CONTINUE;}
        if(!level.hasChunkAt(job.machine.pos()))return WorkSession.Step.CONTINUE;
        if(!level.getBlockState(job.machine.pos()).is(Blocks.FURNACE)||!(level.getBlockEntity(job.machine.pos()) instanceof AbstractFurnaceBlockEntity machine)){end(v,job);return WorkSession.Step.CONTINUE;}
        if(OWNERS.putIfAbsent(job.machine,v.getUUID())!=null&&!v.getUUID().equals(OWNERS.get(job.machine)))return WorkSession.Step.CONTINUE;
        var carry=VillageInventories.carried(v);
        var produced=machine.getItem(2);
        if(!produced.isEmpty()){
            // Every finished product of this role's kind is taken out, also one left by an earlier job.
            if(!accepts(produced)){end(v,job);return WorkSession.Step.IDLE;}
            var item=ItemVariant.of(produced);long amount=produced.getCount();
            if(VillageInventories.nearest(v,home,item,amount).isEmpty())return WorkSession.Step.IDLE;
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
            }return WorkSession.Step.CONTINUE;
        }
        if(!machine.getItem(0).isEmpty()){
            if(fueled(level,machine))return WorkSession.Step.IDLE; // Let the native machine cook.
            var expectedOutput=job.output;
            var fuel=WorkResources.available(level,home).keySet().stream().filter(f->f.toStack().has(DataComponents.COOKING_FUEL)
                &&fuelTargets(v,expectedOutput,f.toStack())&&machine.canPlaceItem(1,f.toStack())).findFirst();
            // A previously collected fuel item is also eligible if its source changed afterward.
            if(fuel.isEmpty())fuel=WorkResources.contents(carry).keySet().stream().filter(f->f.toStack().has(DataComponents.COOKING_FUEL)&&fuelTargets(v,expectedOutput,f.toStack())&&machine.canPlaceItem(1,f.toStack())).findFirst();
            if(fuel.isEmpty())return WorkSession.Step.IDLE;
            if(!WorkResources.gather(v,home,fuel.get(),1,walk))return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            InventoryTransfers.moveExact(carry,slot(machine,level,1),fuel.get(),1,()->Delivery.arrived(v,machine.getBlockPos()));return WorkSession.Step.CONTINUE;
        }
        // Empty input: refill a batch while the village has the input and a chest for the product; else the job is done.
        var ingredient=ItemVariant.of(job.input);
        long stock=WorkResources.available(level,home).getOrDefault(ingredient,0L)+WorkResources.contents(carry).getOrDefault(ingredient,0L);
        if(stock==0||VillageInventories.nearest(v,home,ItemVariant.of(job.output),job.output.getCount()).isEmpty()){end(v,job);return WorkSession.Step.CONTINUE;}
        long batch=Math.min(Math.min(BATCH,stock),job.input.getMaxStackSize());
        if(!WorkResources.gather(v,home,ingredient,batch,walk))return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(v,job.machine.pos())){walk.offer(v,job.machine.pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
        long carried=WorkResources.contents(carry).getOrDefault(ingredient,0L);
        InventoryTransfers.moveExact(carry,slot(machine,level,0),ingredient,carried,()->Delivery.arrived(v,machine.getBlockPos())
            &&level.getBlockEntity(machine.getBlockPos())==machine);
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
