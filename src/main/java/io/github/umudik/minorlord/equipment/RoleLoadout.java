package io.github.umudik.minorlord.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.work.CombatTask;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;

/** Identifies generated role tools separately from real carried items. Real gear is never deleted. */
public final class RoleLoadout {
    private record Gear(EquipmentSlot slot,ItemStack item){
        static final Codec<Gear> CODEC=RecordCodecBuilder.create(i->i.group(EquipmentSlot.CODEC.fieldOf("slot").forGetter(Gear::slot),ItemStack.CODEC.fieldOf("item").forGetter(Gear::item)).apply(i,Gear::new));
    }
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final AttachmentType<List<Gear>> OWNED=AttachmentRegistry.createPersistent(id("role_loadout"),Gear.CODEC.listOf());
    private static final AttachmentType<String> ROLE=AttachmentRegistry.createPersistent(id("loadout_role"),Codec.STRING);
    private static final AttachmentType<List<ItemStack>> PENDING=AttachmentRegistry.createPersistent(id("return_equipment"),ItemStack.CODEC.listOf());
    private static final AttachmentType<Boolean> ACTIVE=AttachmentRegistry.createPersistent(id("role_handoff"),Codec.BOOL);
    private RoleLoadout(){}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("minor_lord",path);}
    public static void initialize(){}
    public static boolean pending(Villager v){return Boolean.TRUE.equals(((AttachmentTarget)v).getAttached(ACTIVE));}
    public static boolean sameRole(Villager v,Role role){return role.name().equals(((AttachmentTarget)v).getAttached(ROLE));}
    public static boolean known(Villager v){return ((AttachmentTarget)v).getAttached(ROLE)!=null;}
    /** Gear this mod generated (older versions); it is never stored in a chest when replaced. */
    public static boolean generated(Villager v,EquipmentSlot slot,ItemStack held){
        var owned=((AttachmentTarget)v).getAttached(OWNED);
        return owned!=null&&owned.stream().anyMatch(g->g.slot==slot&&ItemStack.isSameItemSameComponents(g.item,held));
    }
    public static boolean hasEquipment(Villager v){return SLOTS.stream().anyMatch(s->!v.getItemBySlot(s).isEmpty());}
    public static void begin(Villager v){
        var a=(AttachmentTarget)v;var owned=Optional.ofNullable(a.getAttached(OWNED)).orElse(List.of());
        var returning=new ArrayList<>(Optional.ofNullable(a.getAttached(PENDING)).orElse(List.of()));
        for(var slot:SLOTS){var held=v.getItemBySlot(slot);if(held.isEmpty())continue;
            boolean generated=owned.stream().anyMatch(g->g.slot==slot&&ItemStack.isSameItemSameComponents(g.item,held));
            if(!generated)returning.add(held.copy());
            v.setItemSlot(slot,ItemStack.EMPTY);v.setDropChance(slot,0);
        }
        if(returning.isEmpty())a.removeAttached(PENDING);else a.setAttached(PENDING,List.copyOf(returning));
        a.removeAttached(OWNED);a.removeAttached(ROLE);a.setAttached(ACTIVE,true);
    }
    /** Move one pending real equipment stack into the same entity's now-empty carried inventory. */
    public static boolean stage(Villager v){
        var a=(AttachmentTarget)v;var pending=a.getAttached(PENDING);if(pending==null||pending.isEmpty())return false;
        var next=new ArrayList<>(pending);var remainder=v.getInventory().addItem(next.removeFirst().copy());
        if(!remainder.isEmpty())next.addFirst(remainder);
        if(next.isEmpty())a.removeAttached(PENDING);else a.setAttached(PENDING,List.copyOf(next));return true;
    }
    public static void complete(Villager v,Role role){((AttachmentTarget)v).removeAttached(ACTIVE);if(role!=null)equip(v,role);}
    public static void equip(Villager v,Role role){
        if(sameRole(v,role))return;
        // Warriors, archers and miners equip real gear from the village chests (Armory); nothing is generated for them.
        if(!Armory.uses(role)){Item item=switch(role){case CHESTER->Items.CHEST;case SHEPHERD->Items.SHEARS;case FISHERMAN->Items.FISHING_ROD;default->null;};
            if(item!=null&&v.getMainHandItem().isEmpty())v.setItemSlot(EquipmentSlot.MAINHAND,RoleEquipment.createUnbreakableCopy(new ItemStack(item)));
        }
        var owned=new ArrayList<Gear>();for(var slot:SLOTS)if(!v.getItemBySlot(slot).isEmpty()){owned.add(new Gear(slot,v.getItemBySlot(slot).copy()));v.setDropChance(slot,0);}
        var a=(AttachmentTarget)v;a.setAttached(OWNED,List.copyOf(owned));a.setAttached(ROLE,role.name());
    }
    public static void dropPending(Villager v){
        var a=(AttachmentTarget)v;var pending=a.getAttached(PENDING);
        if(pending!=null&&v.level() instanceof net.minecraft.server.level.ServerLevel level)for(var item:pending)v.spawnAtLocation(level,item.copy());
        a.removeAttached(PENDING);a.removeAttached(ACTIVE);
    }
}
