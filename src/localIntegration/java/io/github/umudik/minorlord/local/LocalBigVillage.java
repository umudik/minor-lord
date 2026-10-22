package io.github.umudik.minorlord.local;

import net.minecraft.server.MinecraftServer;

/** The big playable village: two of every workstation, six warriors, four archers and three miners with chest gear. */
public final class LocalBigVillage {
    public static void run(MinecraftServer server){LocalSpawnVillage.build(server,LocalSpawnVillage.BIG);}
}
