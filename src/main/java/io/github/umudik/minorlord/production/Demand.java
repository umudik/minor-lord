package io.github.umudik.minorlord.production;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.equipment.Armory;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.WorkResources;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/**
 * Gear is made for the villagers who use it: the village keeps as many melee weapons as warriors, bows as archers,
 * pickaxes as miners, axes as timbers, and each armor piece as warriors, archers, miners and timbers together. Counted are the labelled
 * chests plus what members wear. Products nobody uses keep the old per-product limit (StockLimits).
 */
public final class Demand {
    public enum Need {
        MELEE(EquipmentSlot.MAINHAND),BOW(EquipmentSlot.MAINHAND),PICKAXE(EquipmentSlot.MAINHAND),AXE(EquipmentSlot.MAINHAND),
        HEAD(EquipmentSlot.HEAD),CHEST(EquipmentSlot.CHEST),LEGS(EquipmentSlot.LEGS),FEET(EquipmentSlot.FEET);
        public final EquipmentSlot slot;
        Need(EquipmentSlot slot){this.slot=slot;}
        boolean usedBy(Role role){return switch(this){case MELEE->role==Role.WARRIOR;case BOW->role==Role.ARCHER;case PICKAXE->role==Role.MINER;case AXE->role==Role.TIMBER;default->Armory.uses(role);};}
        Role judge(){return switch(this){case BOW->Role.ARCHER;case PICKAXE->Role.MINER;case AXE->Role.TIMBER;default->Role.WARRIOR;};}
    }
    private Demand(){}

    public static Optional<Need> need(HolderLookup.Provider registries,ItemStack stack){
        for(var need:Need.values())if(Armory.score(registries,need.judge(),need.slot,stack)>=0)return Optional.of(need);
        return Optional.empty();
    }
    /** How good this product is for its need (higher first when choosing what to make); 0 for other products. */
    public static double value(HolderLookup.Provider registries,ItemStack stack){
        return need(registries,stack).map(n->Armory.score(registries,n.judge(),n.slot,stack)).orElse(0.0);
    }

    /** How many more the village needs; empty when the product is not gear anyone in the village uses. */
    public static OptionalLong deficit(ServerLevel level,GlobalPos home,ItemStack output){
        var registries=level.registryAccess();var need=need(registries,output);if(need.isEmpty())return OptionalLong.empty();
        var village=UUID.nameUUIDFromBytes(home.toString().getBytes(StandardCharsets.UTF_8));
        long users=0,held=0;
        for(var member:MinorLord.MANAGED_VILLAGERS.members(village).entrySet()){
            if(!need.get().usedBy(member.getValue()))continue;users++;
            if(level.getEntity(member.getKey()) instanceof Villager v&&need(registries,v.getItemBySlot(need.get().slot)).filter(need.get()::equals).isPresent())held++;
        }
        if(users==0)return OptionalLong.empty();
        Set<Object> seen=new HashSet<>();
        for(var chest:VillageInventories.chests(level,home)){
            var endpoint=LiveChestAccess.resolve(level,chest);if(endpoint.isEmpty()||!Collections.disjoint(seen,endpoint.get().inventoryIdentities()))continue;
            seen.addAll(endpoint.get().inventoryIdentities());
            for(var e:WorkResources.contents(endpoint.get()).entrySet())if(need(registries,e.getKey().toStack()).filter(need.get()::equals).isPresent())held+=e.getValue();
        }
        return OptionalLong.of(Math.max(0,users-held));
    }
}
