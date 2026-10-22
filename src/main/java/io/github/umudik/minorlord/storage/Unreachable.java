package io.github.umudik.minorlord.storage;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Places a villager walked toward without getting any closer (a chest in a sealed cave under the village). For
 * FORGET_TICKS they are left out of the village's storage, so nobody counts on them or keeps walking at them.
 */
public final class Unreachable {
    public static final long FORGET_TICKS=1200;
    private static final Map<GlobalPos,Long> UNTIL=new HashMap<>();
    private Unreachable(){}
    public static void report(ServerLevel level,BlockPos pos){UNTIL.put(GlobalPos.of(level.dimension(),pos.immutable()),level.getGameTime()+FORGET_TICKS);}
    public static boolean is(ServerLevel level,BlockPos pos){
        var key=GlobalPos.of(level.dimension(),pos);var until=UNTIL.get(key);if(until==null)return false;
        if(level.getGameTime()<until)return true;
        UNTIL.remove(key);return false;
    }
    public static void clear(){UNTIL.clear();}
}
