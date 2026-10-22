package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/**
 * Word of mouth: a villager knows of a fight, or of loose items lying in the village, only if it saw it or heard of it
 * from a villager of its village within RANGE blocks. News is passed on every RELAY_TICKS and forgotten after
 * FRESH_TICKS without news. Chest events do not use this.
 */
public final class Alarms {
    public static final int RANGE=50,RELAY_TICKS=40,FRESH_TICKS=600;
    /** Items lying this long (not just dropped and about to be picked up) are news; seen within SIGHT blocks. */
    private static final int LYING_TICKS=100,SIGHT=16;
    public enum Kind { ENEMY, ITEMS }
    public record Alarm(BlockPos pos,long heardAt){}
    private static final Map<Kind,Map<UUID,Alarm>> KNOWN=new EnumMap<>(Kind.class);
    /** When a villager last found a place empty: older news is not taken again, so it is not sent back there. */
    private static final Map<Kind,Map<UUID,Long>> RESOLVED=new EnumMap<>(Kind.class);
    static{for(var kind:Kind.values()){KNOWN.put(kind,new HashMap<>());RESOLVED.put(kind,new HashMap<>());}}
    private Alarms(){}

    /** This villager saw it (an enemy, loose items) at pos. */
    public static void raise(Villager v,Kind kind,BlockPos pos){KNOWN.get(kind).put(v.getUUID(),new Alarm(pos.immutable(),v.level().getGameTime()));}
    public static Optional<Alarm> known(Villager v,Kind kind){
        var known=KNOWN.get(kind);var alarm=known.get(v.getUUID());if(alarm==null)return Optional.empty();
        if(v.level().getGameTime()-alarm.heardAt()>FRESH_TICKS){known.remove(v.getUUID());return Optional.empty();}
        return Optional.of(alarm);
    }
    /** The villager went there and found nothing left: it stops passing the news on. */
    public static void resolved(Villager v,Kind kind){
        var alarm=KNOWN.get(kind).remove(v.getUUID());if(alarm!=null)RESOLVED.get(kind).put(v.getUUID(),alarm.heardAt());
    }
    public static void forget(UUID id){for(var kind:Kind.values()){KNOWN.get(kind).remove(id);RESOLVED.get(kind).remove(id);}}

    /** Loose items lying inside the village that this villager can see are news. */
    private static void spot(Villager v,ServerLevel level){
        var home=((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)v).getAttached(io.github.umudik.minorlord.roles.RoleController.HOME);
        var declaration=home==null?null:MinorLord.VILLAGE_SCOPES.active(home).orElse(null);
        if(declaration==null)return;var bounds=declaration.bounds();
        level.getEntitiesOfClass(ItemEntity.class,v.getBoundingBox().inflate(SIGHT),e->e.isAlive()&&e.getAge()>=LYING_TICKS
                &&bounds.contains(e.getBlockX(),e.getBlockZ())&&v.hasLineOfSight(e))
            .stream().findAny().ifPresent(e->raise(v,Kind.ITEMS,e.blockPosition()));
    }
    /** Pass fresher news to managed villagers of the same village within RANGE blocks. */
    public static void relay(Villager v){
        if(v.tickCount%RELAY_TICKS!=0)return;
        var level=(ServerLevel)v.level();var mine=MinorLord.MANAGED_VILLAGERS.find(v.getUUID());if(mine.isEmpty())return;
        spot(v,level);
        List<Villager> neighbours=null;
        for(var kind:Kind.values()){
            var alarm=known(v,kind);if(alarm.isEmpty())continue;
            if(neighbours==null)neighbours=level.getEntitiesOfClass(Villager.class,new AABB(v.blockPosition()).inflate(RANGE),o->o!=v&&o.isAlive()&&o.distanceToSqr(v)<=RANGE*RANGE
                &&MinorLord.MANAGED_VILLAGERS.find(o.getUUID()).filter(a->a.villageId().equals(mine.get().villageId())).isPresent());
            var known=KNOWN.get(kind);var resolved=RESOLVED.get(kind);
            for(var other:neighbours){
                var theirs=known.get(other.getUUID());
                if(resolved.getOrDefault(other.getUUID(),Long.MIN_VALUE)>=alarm.get().heardAt())continue;
                if(theirs==null||theirs.heardAt()<alarm.get().heardAt())known.put(other.getUUID(),alarm.get());
            }
        }
    }
}
