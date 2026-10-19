package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.village.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

/** Actual naturally ticking NPC navigation on a temporary sky platform, with strict local-only cleanup. */
public final class LocalNavigationChecks {
    private static boolean running;
    public static void start(MinecraftServer server)throws Exception{
        if(running)throw new IllegalStateException("Probe already running");
        if(server.getPlayerList().getPlayerCount()!=0){System.out.println("MINOR_LORD_NAV_CHECK skipped: player online; no clock change");return;}
        var level=server.overworld();level.getChunk(0,0);int y=level.getMaxY()-8;
        Map<BlockPos,BlockState> originals=new LinkedHashMap<>();
        for(var p:BlockPos.betweenClosed(new BlockPos(0,y-1,0),new BlockPos(9,y+4,3))){
            if(!level.getBlockState(p).isAir())throw new IllegalStateException("Sky probe space occupied");originals.put(p.immutable(),level.getBlockState(p));
        }
        var clock=server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        var clockState=level.clockManager().getInstance(clock).packState();
        var empty=MinecraftServer.class.getDeclaredField("emptyTicks");empty.setAccessible(true);int oldEmpty=empty.getInt(server);
        boolean wasForced=level.getForceLoadedChunks().contains(0L);
        long startTime=level.getGameTime();
        var home=GlobalPos.of(level.dimension(),new BlockPos(5,y,2));
        var source=new BlockPos(1,y,1);var destination=new BlockPos(7,y,1);var label=destination.east();
        var workerRef=new java.util.concurrent.atomic.AtomicReference<Villager>();
        Runnable cleanup=()->{
            running=false;var worker=workerRef.get();
            if(worker!=null){MinorLord.WORK_SESSIONS.removeFor(worker);MinorLord.MANAGED_VILLAGERS.remove(worker.getUUID());worker.discard();}
            for(var p:List.of(source,destination)){var entity=level.getBlockEntity(p);if(entity instanceof net.minecraft.world.Container container)container.clearContent();if(entity!=null)VillageInventories.unloaded(entity,level);}
            for(var e:originals.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
            MinorLord.VILLAGE_SCOPES.remove(home);MinorLord.VILLAGE_WALLS.forget(home);
            level.clockManager().getInstance(clock).loadFrom(clockState);level.clockManager().setDirty();
            if(!wasForced)level.setChunkForced(0,0,false);
            try{empty.setInt(server,oldEmpty);}catch(IllegalAccessException failure){throw new RuntimeException(failure);}
            System.out.println("MINOR_LORD_NAV_CHECK fixture/NPC/clock/chunk-force state restored");
        };
        try{
        empty.setInt(server,0);level.setChunkForced(0,0,true);level.resetEmptyTime();
        level.clockManager().setTotalTicks(clock,6000);level.clockManager().setPaused(clock,true);
        for(var p:BlockPos.betweenClosed(new BlockPos(0,y-1,0),new BlockPos(9,y-1,3)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        level.setBlock(source,Blocks.CHEST.defaultBlockState(),2);level.setBlock(destination,Blocks.CHEST.defaultBlockState(),2);
        level.setBlock(label,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.EAST),2);
        ((SignBlockEntity)level.getBlockEntity(label)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("iron")).asImmutable(),SignTextSlot.FRONT);
        var from=(ChestBlockEntity)level.getBlockEntity(source);var to=(ChestBlockEntity)level.getBlockEntity(destination);
        level.setBlock(home.pos(),Blocks.OAK_SIGN.defaultBlockState(),2);
        ((SignBlockEntity)level.getBlockEntity(home.pos())).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("village")).setLine(1,Component.literal("5x2")).asImmutable(),SignTextSlot.FRONT);
        VillageInventories.loaded(from,level);VillageInventories.loaded(to,level);from.setItem(0,new ItemStack(Items.IRON_INGOT,17));
        Villager worker=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);
        if(worker==null)throw new IllegalStateException("No villager");
        workerRef.set(worker);
        worker.setPos(1.5,y,2.5);worker.setCustomName(Component.literal("chester"));worker.tickCount=20;
        
        worker.setPersistenceRequired();level.addFreshEntity(worker);running=true;
        int[] ticks={0};
        ServerTickEvents.END_SERVER_TICK.register(current->{
            if(!running||current!=server)return;
            boolean success=to.getItem(0).is(Items.IRON_INGOT)&&to.getItem(0).getCount()==17&&from.getItem(0).isEmpty();
            if(!success&&server.getPlayerList().getPlayerCount()==0&&++ticks[0]<400)return;
            try{
                System.out.println("MINOR_LORD_NAV_CHECK "+(success?"PASS":"FAIL")+" real ticking Chester walked from (1.5,"+y+",2.5) to "+worker.position()+" and delivered "+to.getItem(0)+" in "+ticks[0]+" ticks; activity="+worker.getBrain().getActiveActivities());
                if(!success){
                    var session=MinorLord.WORK_SESSIONS.find(worker.getUUID());
                    System.out.println("MINOR_LORD_NAV_CHECK details session="+session.map(s->s.state())+" carry="+io.github.umudik.minorlord.work.WorkResources.contents(VillageInventories.carried(worker))
                        +" source="+from.getItem(0)+" walk="+worker.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).map(w->w.getTarget().currentBlockPosition())+" chests="+VillageInventories.chests(level,home));
                    session.ifPresent(s->{try{var f=s.getClass().getDeclaredField("task");f.setAccessible(true);var task=f.get(s);var field=task.getClass().getDeclaredField("source");field.setAccessible(true);System.out.println("MINOR_LORD_NAV_CHECK planned source="+field.get(task));}catch(Exception failure){failure.printStackTrace();}});
                }
            }finally{
                cleanup.run();
                if(success)try{LocalMinerNavigationChecks.start(server);}catch(Exception failure){failure.printStackTrace();}
            }
        });
        System.out.println("MINOR_LORD_NAV_CHECK running actual NPC walking test; 400-tick maximum");
        }catch(Exception|Error failure){cleanup.run();throw failure;}
    }
}
