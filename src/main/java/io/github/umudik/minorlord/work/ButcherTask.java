package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.ButcherPolicy;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Approved livestock only, named animals protected, eight adults retained and one cull per five minutes. */
public final class ButcherTask implements WorkSession.Task<Villager>{
    private static final Set<EntityType<?>> SPECIES=Set.of(EntityTypes.COW,EntityTypes.PIG,EntityTypes.CHICKEN,EntityTypes.RABBIT,EntityTypes.SHEEP);
    private static final AttachmentType<Long> NEXT=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","butcher_next_cull"),Codec.LONG);
    private record DeathLoot(UUID animal,List<ItemStack> stacks){}
    private static final ThreadLocal<DeathLoot> DEATH=new ThreadLocal<>();
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();private int wait;private long nextScan;
    public ButcherTask(GlobalPos home){this.home=home;}
    public static void initialize(){}
    /** Called only during the same synchronous native death to reuse exactly the preflighted loot roll. */
    public static boolean emitPlanned(LivingEntity animal,ServerLevel level){
        var loot=DEATH.get();if(loot==null||!loot.animal.equals(animal.getUUID()))return false;
        for(var stack:loot.stacks)animal.spawnAtLocation(level,stack.copy());return true;
    }
    public static boolean eligible(Animal animal){return SPECIES.contains(animal.getType())&&animal.isAlive()&&!animal.isBaby()&&!animal.hasCustomName()
        &&(!(animal instanceof Sheep sheep)||sheep.isSheared());}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();GroundItems.collect(v);if(wait>0){wait--;return WorkSession.Step.CONTINUE;}
        if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
        if(!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_DROPS))return WorkSession.Step.CONTINUE;
        var attached=(AttachmentTarget)v;var next=attached.getAttached(NEXT);
        if((next!=null&&level.getGameTime()<next)||level.getGameTime()<nextScan)return WorkSession.Step.CONTINUE;
        var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        var b=declaration.get().bounds();var box=new AABB(b.minX(),level.getMinY(),b.minZ(),b.maxXExclusive(),level.getMaxY(),b.maxZExclusive());
        var animals=level.getEntitiesOfClass(Animal.class,box,a->SPECIES.contains(a.getType())&&a.isAlive()&&!a.isBaby());
        Map<EntityType<?>,Long> adults=new HashMap<>();for(var animal:animals)adults.merge(animal.getType(),1L,Long::sum);
        var target=animals.stream().filter(a->eligible(a)&&ButcherPolicy.mayCull(adults.get(a.getType()).intValue(),true,a instanceof Sheep,a instanceof Sheep s&&s.isSheared()))
            .min(Comparator.comparingDouble(a->a.distanceToSqr(v))).orElse(null);
        if(target==null){nextScan=level.getGameTime()+200;return WorkSession.Step.CONTINUE;}
        if(v.distanceToSqr(target)>1){walk.offer(v,target.blockPosition(),0.5f,0);return WorkSession.Step.CONTINUE;}
        var table=target.getLootTable();if(table.isEmpty()){nextScan=level.getGameTime()+200;return WorkSession.Step.CONTINUE;}
        var source=level.damageSources().mobAttack(v);List<ItemStack> loot=new ArrayList<>();
        target.dropFromLootTable(level,source,false,table.get(),stack->{if(!stack.isEmpty())loot.add(stack.copy());});
        Map<ItemVariant,Long> products=new LinkedHashMap<>();for(var stack:loot)products.merge(ItemVariant.of(stack),(long)stack.getCount(),Long::sum);
        if(products.isEmpty()||!WorkResources.targets(v,home,products)){nextScan=level.getGameTime()+100;return WorkSession.Step.CONTINUE;}
        try(var simulation=Transaction.openOuter()){
            var carry=VillageInventories.carried(v);for(var e:products.entrySet())if(carry.storage().insert(e.getKey(),e.getValue(),simulation)!=e.getValue())return WorkSession.Step.CONTINUE;
        }
        // All guards and the death run on one server tick. No animal is removed or replaced manually.
        try{DEATH.set(new DeathLoot(target.getUUID(),List.copyOf(loot)));target.hurtServer(level,source,Float.MAX_VALUE);}
        finally{DEATH.remove();}
        if(!target.isAlive()){attached.setAttached(NEXT,level.getGameTime()+6000);wait=16;walk.release(v);}
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
