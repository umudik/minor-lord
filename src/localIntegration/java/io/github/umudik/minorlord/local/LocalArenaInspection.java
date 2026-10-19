package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.roles.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
public final class LocalArenaInspection {
    public static void run(MinecraftServer server){
        if(server.getPort()!=25578)throw new IllegalStateException("Wrong server");var level=server.overworld();
        for(int i=0;i<20;i++){
            int x=180+(i%5)*18,z=477+(i/5)*22;var p=new BlockPos(x+7,64,z+10);if(i==13)p=new BlockPos(x+10,64,z+6);
            System.out.println("ARENA_POI room="+(i+1)+" pos="+p+" block="+level.getBlockState(p)+" poi="+level.getPoiManager().getType(p));
        }
        for(var e:level.getAllEntities())if(e instanceof Villager v&&v.hasCustomName()&&v.getX()>=175&&v.getX()<275&&v.getZ()>=472&&v.getZ()<572)System.out.println("ARENA_VILLAGER name="+v.getCustomName()+" finalized="+v.getVillagerDataFinalized()+" profession="+v.getVillagerData().profession().unwrapKey()+" role="+NaturalProfessionRoles.find(v.getVillagerData().profession())+" job="+v.getBrain().getMemory(MemoryModuleType.JOB_SITE)+" potential="+v.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)+" ticks="+v.tickCount);
    }
}
