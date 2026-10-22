package io.github.umudik.minorlord.local;

import net.minecraft.server.MinecraftServer;

/** Reopens the spawn and big village stairways in one piece, with their entrance signs. */
public final class LocalMineRepair {
    public static void run(MinecraftServer server){
        LocalSpawnVillage.repairMine(server,LocalSpawnVillage.SPAWN);
        LocalSpawnVillage.repairMine(server,LocalSpawnVillage.BIG);
    }
}
