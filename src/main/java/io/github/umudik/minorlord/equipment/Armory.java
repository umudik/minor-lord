package io.github.umudik.minorlord.equipment;

import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.Delivery;
import io.github.umudik.minorlord.work.TaskWalkTarget;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;

/**
 * Warriors, archers and miners equip the best real gear in their village chests, judged by vanilla values: attack
 * damage and Sharpness, bow enchantments, pickaxe speed on stone and Efficiency, armor, toughness and Protection.
 * The replaced piece goes back into the same chest (or is carried home, or dropped), so nothing is lost. Gear from
 * chests wears like vanilla and drops when its wearer dies. Generated gear from older versions is never stored.
 */
public final class Armory {
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final long RECHECK_TICKS=200;
    private record Upgrade(BlockPos chest,ItemVariant item,EquipmentSlot slot){}
    private final Role role;private Upgrade pending;private long nextCheck;
    public Armory(Role role){this.role=role;}

    public static boolean uses(Role role){return role==Role.WARRIOR||role==Role.ARCHER||role==Role.MINER;}

    private static int level(HolderLookup.Provider world,ItemStack stack,ResourceKey<Enchantment> key){
        return world.lookupOrThrow(Registries.ENCHANTMENT).get(key).map(h->stack.getEnchantments().getLevel(h)).orElse(0);
    }
    /** How good a stack is in a slot for a role; negative when it does not belong there. */
    public static double score(HolderLookup.Provider world,Role role,EquipmentSlot slot,ItemStack stack){
        if(stack.isEmpty())return -1;
        if(slot==EquipmentSlot.MAINHAND){
            if(role==Role.ARCHER){
                if(!stack.is(Items.BOW))return -1;
                return 1+level(world,stack,Enchantments.POWER)+0.3*level(world,stack,Enchantments.PUNCH)
                    +0.5*level(world,stack,Enchantments.FLAME)+0.5*level(world,stack,Enchantments.INFINITY);
            }
            if(role==Role.MINER){
                if(!stack.is(ItemTags.PICKAXES))return -1;
                int efficiency=level(world,stack,Enchantments.EFFICIENCY);
                return stack.getDestroySpeed(Blocks.STONE.defaultBlockState())+(efficiency>0?efficiency*efficiency+1:0);
            }
            if(!(stack.is(ItemTags.SWORDS)||stack.is(ItemTags.AXES)||stack.is(Items.MACE)||stack.is(Items.TRIDENT)))return -1;
            return meleeValue(world,stack);
        }
        var equippable=stack.get(DataComponents.EQUIPPABLE);if(equippable==null||equippable.slot()!=slot)return -1;
        var modifiers=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        double armor=modifiers.compute(Attributes.ARMOR,0,slot),toughness=modifiers.compute(Attributes.ARMOR_TOUGHNESS,0,slot);
        if(armor<=0&&toughness<=0)return -1;
        return armor+toughness+0.5*level(world,stack,Enchantments.PROTECTION);
    }
    /** Hit damage: the weapon's attack damage plus vanilla Sharpness (0.5 per level + 0.5). */
    public static double meleeValue(HolderLookup.Provider world,ItemStack stack){
        int sharpness=level(world,stack,Enchantments.SHARPNESS);
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND)
            +(sharpness>0?0.5*sharpness+0.5:0);
    }
    private static List<EquipmentSlot> slots(){var all=new ArrayList<>(List.of(EquipmentSlot.MAINHAND));all.addAll(List.of(ARMOR));return all;}

    /** The single most improving swap available in the village chests right now. */
    private Optional<Upgrade> find(Villager v,GlobalPos home){
        var world=(ServerLevel)v.level();var registries=world.registryAccess();Upgrade best=null;double gain=0,distance=Double.MAX_VALUE;
        for(var chest:VillageInventories.chests(world,home)){
            var endpoint=LiveChestAccess.resolve(world,chest);if(endpoint.isEmpty())continue;
            for(var view:endpoint.get().storage()){
                if(view.isResourceBlank()||view.getAmount()<=0)continue;
                var stack=view.getResource().toStack();
                for(var slot:slots()){
                    double offer=score(registries,role,slot,stack);if(offer<0)continue;
                    double improvement=offer-score(registries,role,slot,v.getItemBySlot(slot));
                    double d=chest.distToCenterSqr(v.position());
                    if(improvement>gain+1e-6||improvement>1e-6&&Math.abs(improvement-gain)<=1e-6&&d<distance){
                        best=new Upgrade(chest.immutable(),view.getResource(),slot);gain=improvement;distance=d;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** True while walking to a chest or swapping gear; checks the chests at most every RECHECK_TICKS. */
    public boolean step(Villager v,GlobalPos home,TaskWalkTarget walk){
        var world=(ServerLevel)v.level();long now=world.getGameTime();
        if(pending==null){
            if(now<nextCheck)return false;
            nextCheck=now+RECHECK_TICKS;pending=find(v,home).orElse(null);
            if(pending==null)return false;
        }
        var upgrade=pending;
        if(!Delivery.arrived(v,upgrade.chest())){walk.offer(v,upgrade.chest(),0.5f,1);return true;}
        walk.release(v);pending=null;
        var endpoint=LiveChestAccess.resolve(world,upgrade.chest());if(endpoint.isEmpty())return true;
        var old=v.getItemBySlot(upgrade.slot());boolean keepOld=!old.isEmpty()&&!RoleLoadout.generated(v,upgrade.slot(),old);
        try(var tx=Transaction.openOuter()){
            if(endpoint.get().storage().extract(upgrade.item(),1,tx)!=1)return true;
            long stored=keepOld?endpoint.get().storage().insert(ItemVariant.of(old),old.getCount(),tx):0;
            tx.commit();
            if(keepOld&&stored<old.getCount()){
                var rest=old.copyWithCount(old.getCount()-(int)stored);
                var left=v.getInventory().addItem(rest);if(!left.isEmpty())v.spawnAtLocation(world,left);
            }
        }
        v.setItemSlot(upgrade.slot(),upgrade.item().toStack());
        v.setDropChance(upgrade.slot(),2.0f); // real gear always drops intact when its wearer dies
        VillageInventories.changed(world,upgrade.chest());
        nextCheck=now; // another slot may improve too
        return true;
    }
}
