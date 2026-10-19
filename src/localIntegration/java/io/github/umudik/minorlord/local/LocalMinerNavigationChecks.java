package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.mining.*;
import io.github.umudik.minorlord.village.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** A freshly spawned, initially employed villager must naturally walk and mine outside its home. */
public final class LocalMinerNavigationChecks {
    private static boolean running;
    public static void start(MinecraftServer server)throws Exception{
        if(server.getPlayerList().getPlayerCount()!=0){System.out.println("MINOR_LORD_MINER_NAV skipped: player online");return;}
        if(running)throw new IllegalStateException("Miner fixture already active");
        var level=server.overworld();level.getChunk(0,0);int y=level.getMaxY()-20;
        var home=GlobalPos.of(level.dimension(),new BlockPos(2,y,2));var mine=new BlockPos(8,y+1,6);var target=new BlockPos(8,y,7);
        Map<BlockPos,BlockState> original=new LinkedHashMap<>();
        for(var p:BlockPos.betweenClosed(new BlockPos(6,y-1,2),new BlockPos(10,y+3,11))){
            if(!level.getBlockState(p).isAir())throw new IllegalStateException("Miner sky fixture occupied");original.put(p.immutable(),level.getBlockState(p));
        }
        for(var p:BlockPos.betweenClosed(new BlockPos(0,y-1,0),new BlockPos(3,y+4,3))){
            if(!level.getBlockState(p).isAir())throw new IllegalStateException("Miner village fixture occupied");original.put(p.immutable(),level.getBlockState(p));
        }
        var clock=server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        var clockState=level.clockManager().getInstance(clock).packState();
        var empty=MinecraftServer.class.getDeclaredField("emptyTicks");empty.setAccessible(true);int oldEmpty=empty.getInt(server);
        boolean wasForced=level.getForceLoadedChunks().contains(0L);Villager[] actor={null};
        Runnable cleanup=()->{
            running=false;if(actor[0]!=null){MinorLord.WORK_SESSIONS.removeFor(actor[0]);MinorLord.MANAGED_VILLAGERS.remove(actor[0].getUUID());actor[0].discard();}
            for(var e:original.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
            MineMarkers.remove(GlobalPos.of(level.dimension(),mine));VillageData.mine(server,GlobalPos.of(level.dimension(),mine),Optional.empty());
            MinorLord.VILLAGE_SCOPES.remove(home);MinorLord.VILLAGE_WALLS.forget(home);
            level.clockManager().getInstance(clock).loadFrom(clockState);level.clockManager().setDirty();if(!wasForced)level.setChunkForced(0,0,false);
            try{empty.setInt(server,oldEmpty);}catch(Exception failure){throw new RuntimeException(failure);}
            System.out.println("MINOR_LORD_MINER_NAV fixture/NPC/clock/chunk-force state restored");
        };
        try{
            empty.setInt(server,0);level.setChunkForced(0,0,true);level.resetEmptyTime();level.clockManager().setTotalTicks(clock,6000);level.clockManager().setPaused(clock,true);
            for(var p:BlockPos.betweenClosed(new BlockPos(6,y-1,2),new BlockPos(10,y-1,11)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            for(var p:BlockPos.betweenClosed(new BlockPos(0,y-1,0),new BlockPos(3,y-1,3)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            for(var p:BlockPos.betweenClosed(new BlockPos(7,y,7),new BlockPos(9,y+3,11)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            level.setBlock(mine,Blocks.OAK_SIGN.defaultBlockState(),2);
            ((SignBlockEntity)level.getBlockEntity(mine)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("mine")).setLine(1,Component.literal("16")).asImmutable(),SignTextSlot.FRONT);
            level.setBlock(home.pos(),Blocks.OAK_SIGN.defaultBlockState(),2);
            ((SignBlockEntity)level.getBlockEntity(home.pos())).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("village")).setLine(1,Component.literal("2x2")).asImmutable(),SignTextSlot.FRONT);
            var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);actor[0]=v;
            v.setPos(8.5,y,3.5);v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.FARMER));
            v.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            v.setCustomName(Component.literal("miner"));v.setPersistenceRequired();level.addFreshEntity(v);running=true;
            int[] ticks={0};
            ServerTickEvents.END_SERVER_TICK.register(current->{
                if(!running||current!=server)return;
                long drops=io.github.umudik.minorlord.work.WorkResources.contents(io.github.umudik.minorlord.storage.VillageInventories.carried(v)).getOrDefault(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COBBLESTONE),0L);
                boolean swordPreserved=false;for(int slot=0;slot<v.getInventory().getContainerSize();slot++)if(v.getInventory().getItem(slot).is(Items.IRON_SWORD))swordPreserved=true;
                boolean success=drops>=3&&level.getBlockState(target.above(2)).isAir()&&v.getZ()>5&&v.getMainHandItem().is(Items.DIAMOND_PICKAXE)&&swordPreserved;
                if(!success&&server.getPlayerList().getPlayerCount()==0&&++ticks[0]<400)return;
                try{System.out.println("MINOR_LORD_MINER_NAV "+(success?"PASS":"FAIL")+" fresh farmer named Miner outside village, walked (8.5,"+y+",3.5) -> "+v.position()+", real mined drops="+drops+", ticks="+ticks[0]+", profession="+v.getVillagerData().profession().unwrapKey()+", activity="+v.getBrain().getActiveActivities()+", session="+MinorLord.WORK_SESSIONS.find(v.getUUID()).map(s->s.state())+", walk="+v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET).map(w->w.getTarget().currentBlockPosition()));}
                finally{cleanup.run();}
            });
            System.out.println("MINOR_LORD_MINER_NAV running fresh NPC natural walking/mining, no manual task ticks or actor teleport");
        }catch(Exception|Error failure){cleanup.run();throw failure;}
    }
}
