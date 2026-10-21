package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Native equipment, damage and arrows; executes only through the normal guarded WORK session. */
public final class CombatTask implements WorkSession.Task<Villager> {
    public static final AttachmentType<Boolean> ARROW=AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("minor_lord","hostile_only_arrow"),Codec.BOOL);
    private final GlobalPos home;private final Role role;private final TaskWalkTarget walk=new TaskWalkTarget();private long nextAttack;
    private final io.github.umudik.minorlord.equipment.Armory armory;private final Loot loot;
    public CombatTask(GlobalPos home,Role role){this.home=home;this.role=role;this.armory=new io.github.umudik.minorlord.equipment.Armory(role);this.loot=new Loot(role);}
    public static void initialize(){}
    public static boolean hostile(Entity e){return e instanceof LivingEntity living&&e instanceof Enemy&&living.isAlive();}
    /** When any soldier last saw each villager; shared, so several soldiers split the rounds between them. */
    private static final Map<UUID,Long> CHECKED=new HashMap<>();
    /** The villager each soldier is walking to, so two soldiers never check the same one. */
    private static final Map<UUID,UUID> CLAIMED=new HashMap<>();
    private static final int LOOK=8,RECHECK_TICKS=200,MARGIN=16;
    private UUID patrolTarget;
    public static void forget(UUID id){CHECKED.remove(id);CLAIMED.remove(id);}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        var b=declaration.get().bounds();var alarm=Alarms.known(v);
        // Enemies it can see, around the village or anywhere nearby once it answered an alarm. A mob in a cave
        // under the rock is not seen, so it is not chased.
        var candidates=level.getEntitiesOfClass(LivingEntity.class,v.getBoundingBox().inflate(24),e->hostile(e)
            &&(alarm.isPresent()||inArea(b,e.blockPosition()))&&v.hasLineOfSight(e));
        var target=candidates.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(v))).orElse(null);
        // Without a usable weapon (an archer without a bow) the soldier arms itself first.
        if(io.github.umudik.minorlord.equipment.Armory.score(level.registryAccess(),role,EquipmentSlot.MAINHAND,v.getMainHandItem())<0
            &&armory.step(v,home,walk))return WorkSession.Step.CONTINUE;
        if(target==null){
            // Word of a fight: go there; arriving with no enemy in sight means it is over for this soldier.
            if(alarm.isPresent()){
                var at=alarm.get().pos();
                if(at.distToCenterSqr(v.position())<=36)Alarms.resolved(v);
                else{walk.offer(v,at,0.7f,2);return WorkSession.Step.CONTINUE;}
            }
            // Quiet: gather and bring home the loot, then better gear, then the rounds.
            if(!loot.step(v,home,walk)&&!armory.step(v,home,walk))patrol(v,level,b);
            return WorkSession.Step.CONTINUE;
        }
        Alarms.raise(v,target.blockPosition());CLAIMED.remove(v.getUUID());patrolTarget=null;
        double range=role==Role.ARCHER?12:2;
        if(target.distanceToSqr(v)>range*range||!v.hasLineOfSight(target)){walk.offer(v,target.blockPosition(),0.6f,1);return WorkSession.Step.CONTINUE;}
        walk.release(v);v.getLookControl().setLookAt(target,30,30);
        if(level.getGameTime()<nextAttack)return WorkSession.Step.CONTINUE;
        if(role==Role.WARRIOR){
            var weapon=v.getMainHandItem();var source=weapon.getDamageSource(v);
            double base=weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND);
            float damage=EnchantmentHelper.modifyDamage(level,weapon,target,source,(float)base);
            if(hostile(target)&&target.hurtServer(level,source,damage)){
                EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,source,weapon);
                if(!weapon.isEmpty())weapon.hurtAndBreak(1,v,EquipmentSlot.MAINHAND);
            }
            v.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);nextAttack=level.getGameTime()+20;
        }else if(v.getMainHandItem().is(Items.BOW)){
            var ammo=ItemVariant.of(Items.ARROW);var carry=VillageInventories.carried(v);
            if(!WorkResources.gather(v,home,ammo,1,walk))return WorkSession.Step.CONTINUE;
            try(var tx=Transaction.openOuter()){
                if(carry.storage().extract(ammo,1,tx)!=1)return WorkSession.Step.CONTINUE;
                var arrow=new Arrow(level,v,new ItemStack(Items.ARROW),v.getMainHandItem());arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
                ((AttachmentTarget)arrow).setAttached(ARROW,true);
                double dx=target.getX()-v.getX(),dz=target.getZ()-v.getZ(),dy=target.getY(0.5)-arrow.getY();
                Projectile.spawnProjectileUsingShoot(arrow,level,v.getMainHandItem(),dx,dy+Math.sqrt(dx*dx+dz*dz)*0.2,dz,1.6f,4);
                tx.commit();nextAttack=level.getGameTime()+30;
                v.getMainHandItem().hurtAndBreak(1,v,EquipmentSlot.MAINHAND);
            }
        }
        return WorkSession.Step.CONTINUE;
    }
    private static boolean inArea(io.github.umudik.minorlord.village.VillageBounds b,BlockPos p){
        return p.getX()>=b.minX()-MARGIN&&p.getX()<b.maxXExclusive()+MARGIN&&p.getZ()>=b.minZ()-MARGIN&&p.getZ()<b.maxZExclusive()+MARGIN;
    }
    /**
     * Rounds without a route: walk to the village villager seen longest ago. One look covers every villager in sight
     * within LOOK blocks, so a group needs one visit; a villager just seen is not visited again for RECHECK_TICKS.
     */
    private void patrol(Villager v,ServerLevel level,io.github.umudik.minorlord.village.VillageBounds b){
        long now=level.getGameTime();
        var target=patrolTarget==null?null:level.getEntity(patrolTarget) instanceof Villager t&&t.isAlive()?t:null;
        if(target==null){target=choose(v,level,b,now);patrolTarget=target==null?null:target.getUUID();}
        if(target==null){CLAIMED.remove(v.getUUID());walk.release(v);return;}
        CLAIMED.put(v.getUUID(),target.getUUID());
        if(v.distanceToSqr(target)<=LOOK*LOOK&&v.hasLineOfSight(target)){
            for(var seen:level.getEntitiesOfClass(Villager.class,v.getBoundingBox().inflate(LOOK),o->o!=v&&v.hasLineOfSight(o)))CHECKED.put(seen.getUUID(),now);
            CLAIMED.remove(v.getUUID());patrolTarget=null;walk.release(v);return;
        }
        walk.offer(v,target.blockPosition(),0.5f,3);
    }
    private Villager choose(Villager v,ServerLevel level,io.github.umudik.minorlord.village.VillageBounds b,long now){
        var mine=MinorLord.MANAGED_VILLAGERS.find(v.getUUID());
        var area=new net.minecraft.world.phys.AABB(b.minX()-MARGIN,level.getMinY(),b.minZ()-MARGIN,b.maxXExclusive()+MARGIN,level.getMaxY(),b.maxZExclusive()+MARGIN);
        var taken=new HashSet<>(CLAIMED.values());CLAIMED.forEach((soldier,villager)->{if(soldier.equals(v.getUUID()))taken.remove(villager);});
        return level.getEntitiesOfClass(Villager.class,area,o->o!=v&&o.isAlive()&&!io.github.umudik.minorlord.roles.RoleController.soldier(o)
                &&!taken.contains(o.getUUID())&&now-CHECKED.getOrDefault(o.getUUID(),Long.MIN_VALUE/2)>=RECHECK_TICKS
                &&(MinorLord.MANAGED_VILLAGERS.find(o.getUUID()).map(a->mine.isPresent()&&a.villageId().equals(mine.get().villageId())).orElse(b.contains(o.getBlockX(),o.getBlockZ()))))
            .stream().min(Comparator.<Villager>comparingLong(o->CHECKED.getOrDefault(o.getUUID(),Long.MIN_VALUE/2)).thenComparingDouble(o->o.distanceToSqr(v))).orElse(null);
    }
    @Override public void pause(Villager v){walk.release(v);CLAIMED.remove(v.getUUID());}
}
