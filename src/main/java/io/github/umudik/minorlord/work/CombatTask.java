package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.equipment.RoleEquipment;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
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
    private static final Item[] WEAPONS={Items.WOODEN_SWORD,Items.STONE_SWORD,Items.IRON_SWORD,Items.GOLDEN_SWORD,Items.DIAMOND_SWORD,Items.NETHERITE_SWORD};
    private static final Item[][] ARMOR={
        {Items.LEATHER_HELMET,Items.CHAINMAIL_HELMET,Items.IRON_HELMET,Items.GOLDEN_HELMET,Items.DIAMOND_HELMET,Items.NETHERITE_HELMET},
        {Items.LEATHER_CHESTPLATE,Items.CHAINMAIL_CHESTPLATE,Items.IRON_CHESTPLATE,Items.GOLDEN_CHESTPLATE,Items.DIAMOND_CHESTPLATE,Items.NETHERITE_CHESTPLATE},
        {Items.LEATHER_LEGGINGS,Items.CHAINMAIL_LEGGINGS,Items.IRON_LEGGINGS,Items.GOLDEN_LEGGINGS,Items.DIAMOND_LEGGINGS,Items.NETHERITE_LEGGINGS},
        {Items.LEATHER_BOOTS,Items.CHAINMAIL_BOOTS,Items.IRON_BOOTS,Items.GOLDEN_BOOTS,Items.DIAMOND_BOOTS,Items.NETHERITE_BOOTS}};
    private final GlobalPos home;private final Role role;private final TaskWalkTarget walk=new TaskWalkTarget();private long nextAttack;
    public CombatTask(GlobalPos home,Role role){this.home=home;this.role=role;}
    public static void initialize(){}
    public static boolean hostile(Entity e){return e instanceof LivingEntity living&&e instanceof Enemy&&living.isAlive();}
    private static void equip(Villager v,EquipmentSlot slot,Item item){
        if(!v.getItemBySlot(slot).isEmpty())return;
        var level=(ServerLevel)v.level();var template=new ItemStack(item);
        var pool=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
            .filter(e->!e.is(EnchantmentTags.CURSE)).map(e->(Holder<net.minecraft.world.item.enchantment.Enchantment>)e);
        var stack=EnchantmentHelper.enchantItem(v.getRandom(),template,1+v.getRandom().nextInt(30),pool);
        v.setItemSlot(slot,RoleEquipment.createUnbreakableCopy(stack));v.setDropChance(slot,0);
    }
    public static void equip(Villager v,Role role){
        equip(v,EquipmentSlot.MAINHAND,role==Role.ARCHER?Items.BOW:WEAPONS[v.getRandom().nextInt(WEAPONS.length)]);
        var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        for(int i=0;i<slots.length;i++)equip(v,slots[i],ARMOR[i][v.getRandom().nextInt(ARMOR[i].length)]);
    }
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        if(role==Role.ARCHER&&!WorkResources.gather(v,home,ItemVariant.of(Items.ARROW),1,walk))return WorkSession.Step.CONTINUE;
        var b=declaration.get().bounds();
        var candidates=level.getEntitiesOfClass(LivingEntity.class,v.getBoundingBox().inflate(24),e->hostile(e)&&b.contains(e.blockPosition().getX(),e.blockPosition().getZ()));
        var target=candidates.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(v))).orElse(null);
        if(target==null){walk.release(v);return WorkSession.Step.CONTINUE;}
        double range=role==Role.ARCHER?12:2;
        if(target.distanceToSqr(v)>range*range||!v.hasLineOfSight(target)){walk.offer(v,target.blockPosition(),0.6f,1);return WorkSession.Step.CONTINUE;}
        walk.release(v);v.getLookControl().setLookAt(target,30,30);
        if(level.getGameTime()<nextAttack)return WorkSession.Step.CONTINUE;
        if(role==Role.WARRIOR){
            var weapon=v.getMainHandItem();var source=weapon.getDamageSource(v);
            double base=weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND);
            float damage=EnchantmentHelper.modifyDamage(level,weapon,target,source,(float)base);
            if(hostile(target)&&target.hurtServer(level,source,damage))EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,source,weapon);
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
            }
        }
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
