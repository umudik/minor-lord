package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.mining.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
public final class LocalRoleDiagnostics {
    public static void run(MinecraftServer server){
        if(server.getPort()!=25578)throw new IllegalStateException("Local diagnostics only");
        System.out.println("MINOR_LORD_ROLE_DIAG villages="+MinorLord.VILLAGE_SCOPES.snapshot()+" mines="+MineMarkers.snapshot());
        var clock=server.overworld().clockManager().getInstance(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD));System.out.println("MINOR_LORD_CLOCK ticks="+clock.totalTicks()+" day="+Math.floorMod(clock.totalTicks(),24000)+" paused="+clock.isPaused());var seen=new java.util.HashSet<net.minecraft.core.GlobalPos>();int signs=0;
        for(var player:server.getPlayerList().getPlayers()){
            var level=(net.minecraft.server.level.ServerLevel)player.level();var chunkPos=player.chunkPosition();
            for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
                var chunk=level.getChunkSource().getChunkNow(chunkPos.x()+dx,chunkPos.z()+dz);if(chunk==null)continue;
                for(var entity:chunk.getBlockEntities().values())if(entity instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign&&seen.add(net.minecraft.core.GlobalPos.of(level.dimension(),sign.getBlockPos()))&&signs++<40){
                    for(var side:net.minecraft.world.level.block.entity.SignTextSlot.values())System.out.println("MINOR_LORD_SIGN_DIAG "+sign.getBlockPos()+" "+side+" "+sign.getText(side).getMessages(false).stream().map(net.minecraft.network.chat.Component::getString).toList());
                }
            }
        }
        for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof Villager v&&v.hasCustomName()){
            var session=MinorLord.WORK_SESSIONS.find(v.getUUID());
            System.out.println("MINOR_LORD_ROLE_DIAG "+v.getUUID()+" name="+v.getCustomName().getString()+" pos="+v.position()+" profession="+v.getVillagerData().profession()+" home="+((AttachmentTarget)v).getAttached(RoleController.HOME)
                +" chests="+java.util.Optional.ofNullable(((AttachmentTarget)v).getAttached(RoleController.HOME)).map(h->io.github.umudik.minorlord.storage.VillageInventories.chests(level,h))+" active="+v.getBrain().getActiveActivities()+" sleeping="+v.isSleeping()+" hand="+v.getMainHandItem()+" session="+session.map(s->s.state().toString())
                +" walk="+v.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(w->w.getTarget().currentBlockPosition())+" pathDone="+v.getNavigation().isDone()+" mine="+MineMarkers.nearest(v,site->true));
        }
    }
}
