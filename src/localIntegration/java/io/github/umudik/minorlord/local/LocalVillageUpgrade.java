package io.github.umudik.minorlord.local;

import net.minecraft.server.MinecraftServer;

/** Upgrades the spawn and big villages: deep mine to y-16 and timbers with axes. */
public final class LocalVillageUpgrade {
    public static void run(MinecraftServer server){
        LocalSpawnVillage.upgrade(server,LocalSpawnVillage.SPAWN);
        LocalSpawnVillage.upgrade(server,LocalSpawnVillage.BIG);
    }
}
