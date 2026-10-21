package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/**
 * Word of mouth: a villager knows of a fight only if it saw one or heard of it from a villager within RANGE blocks.
 * Knowledge is passed on every RELAY_TICKS and forgotten after FRESH_TICKS without news. Chest events do not use this.
 */
public final class Alarms {
    public static final int RANGE=50,RELAY_TICKS=40,FRESH_TICKS=600;
    public record Alarm(BlockPos pos,long heardAt){}
    private static final Map<UUID,Alarm> KNOWN=new HashMap<>();
    private Alarms(){}

    /** This villager saw an enemy or is fighting at pos. */
    public static void raise(Villager v,BlockPos pos){KNOWN.put(v.getUUID(),new Alarm(pos.immutable(),v.level().getGameTime()));}
    public static Optional<Alarm> known(Villager v){
        var alarm=KNOWN.get(v.getUUID());if(alarm==null)return Optional.empty();
        if(v.level().getGameTime()-alarm.heardAt()>FRESH_TICKS){KNOWN.remove(v.getUUID());return Optional.empty();}
        return Optional.of(alarm);
    }
    /** The soldier went there and found no enemy left: it stops passing the news on. */
    public static void resolved(Villager v){KNOWN.remove(v.getUUID());}
    public static void forget(UUID id){KNOWN.remove(id);}

    /** Pass fresher news to managed villagers of the same village within RANGE blocks. */
    public static void relay(Villager v){
        if(v.tickCount%RELAY_TICKS!=0)return;
        var alarm=known(v);if(alarm.isEmpty())return;
        var level=(ServerLevel)v.level();var mine=MinorLord.MANAGED_VILLAGERS.find(v.getUUID());if(mine.isEmpty())return;
        for(var other:level.getEntitiesOfClass(Villager.class,new AABB(v.blockPosition()).inflate(RANGE),o->o!=v&&o.isAlive()&&o.distanceToSqr(v)<=RANGE*RANGE)){
            if(MinorLord.MANAGED_VILLAGERS.find(other.getUUID()).filter(a->a.villageId().equals(mine.get().villageId())).isEmpty())continue;
            var theirs=KNOWN.get(other.getUUID());
            if(theirs==null||theirs.heardAt()<alarm.get().heardAt())KNOWN.put(other.getUUID(),alarm.get());
        }
    }
}
