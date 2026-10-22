package io.github.umudik.minorlord.work;

import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Activation range: a villager within NEAR blocks of a player thinks (vanilla brain and Minor Lord work) every tick;
 * farther away every INTERVAL ticks. Walking, physics and hunger still run every tick. Timed work counts the game
 * ticks a brain step stands for, so a block takes as long to break far away as near a player.
 */
public final class Activation {
    public static final int NEAR=32,INTERVAL=5;
    private Activation(){}
    public static boolean near(Villager v){
        for(var player:v.level().players())if(!player.isSpectator()&&player.distanceToSqr(v)<NEAR*NEAR)return true;
        return false;
    }
    public static boolean thinks(Villager v){return near(v)||(v.tickCount+v.getId())%INTERVAL==0;}
    /** Game ticks one brain step of this villager stands for. */
    public static int elapsed(Villager v){return near(v)?1:INTERVAL;}
}
