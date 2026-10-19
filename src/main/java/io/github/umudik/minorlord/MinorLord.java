package io.github.umudik.minorlord;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import io.github.umudik.minorlord.village.LiveVillageMarkers;
import io.github.umudik.minorlord.village.VillageScopes;
import io.github.umudik.minorlord.village.VillageWalls;
import io.github.umudik.minorlord.roles.ManagedVillagers;
import io.github.umudik.minorlord.work.ManagedWorkSessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server entry point. Role AI is deliberately not installed by this first foundation. */
public final class MinorLord implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("minor_lord");
    public static final ManagedVillagers MANAGED_VILLAGERS = new ManagedVillagers();
    public static final ManagedWorkSessions WORK_SESSIONS = new ManagedWorkSessions(MANAGED_VILLAGERS);
    public static final VillageScopes<GlobalPos, ResourceKey<Level>> VILLAGE_SCOPES = new VillageScopes<>();
    public static final VillageWalls VILLAGE_WALLS = new VillageWalls();

    @Override
    public void onInitialize() {
        io.github.umudik.minorlord.roles.RoleController.initialize();
        io.github.umudik.minorlord.work.FurnaceTask.initialize();
        ServerLifecycleEvents.SERVER_STARTED.register(io.github.umudik.minorlord.village.VillageData::restore);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level,chunk,newlyGenerated)->VILLAGE_WALLS.queueChunk(level,chunk.getPos()));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(entity instanceof net.minecraft.world.entity.npc.villager.Villager villager
                &&((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)villager).hasAttached(io.github.umudik.minorlord.roles.RoleController.HOME)){
                net.minecraft.world.Containers.dropContents(villager.level(),villager,villager.getInventory());
                io.github.umudik.minorlord.equipment.RoleLoadout.dropPending(villager);
                villager.getInventory().clearContent();WORK_SESSIONS.removeFor(villager);MANAGED_VILLAGERS.remove(villager.getUUID());
                io.github.umudik.minorlord.work.FurnaceTask.release(villager);
                io.github.umudik.minorlord.work.BrewingTask.release(villager);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server->io.github.umudik.minorlord.storage.VillageInventories.flushChestEvents());
        ServerTickEvents.END_SERVER_TICK.register(VILLAGE_WALLS::tick);
        ServerTickEvents.END_SERVER_TICK.register(server->io.github.umudik.minorlord.roles.RoleController.flush());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(entity instanceof net.minecraft.world.entity.npc.villager.Villager v)io.github.umudik.minorlord.roles.RoleController.request(v,level);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if(entity instanceof net.minecraft.world.entity.npc.villager.Villager v)io.github.umudik.minorlord.roles.RoleController.unloaded(v);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            io.github.umudik.minorlord.storage.VillageInventories.loaded(entity,level);
            if (entity instanceof SignBlockEntity sign) LiveVillageMarkers.refresh(sign,level);
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            io.github.umudik.minorlord.storage.VillageInventories.unloaded(entity,level);
            if (entity instanceof SignBlockEntity sign) LiveVillageMarkers.forget(sign, level);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            io.github.umudik.minorlord.roles.RoleController.clear();
            WORK_SESSIONS.clear();
            MANAGED_VILLAGERS.clear();
            VILLAGE_SCOPES.clear();
            VILLAGE_WALLS.clear();
            io.github.umudik.minorlord.storage.VillageInventories.clear();
            io.github.umudik.minorlord.mining.MineMarkers.clear();
            io.github.umudik.minorlord.work.FurnaceTask.clear();
            io.github.umudik.minorlord.work.BrewingTask.clear();
        });
        LOGGER.info("Minor Lord loaded: Village walls, named Miner/Chester and native recipe crafting work enabled.");
    }
}
