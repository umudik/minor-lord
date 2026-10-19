package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.roles.RoleController;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
/** Gates only native workplace acquisition; bed, bell, movement and fear behaviors remain unchanged. */
public final class MinerJobGate implements BehaviorControl<PathfinderMob>{
    private final BehaviorControl<PathfinderMob> original;
    public MinerJobGate(BehaviorControl<PathfinderMob> original){this.original=original;}
    @Override public Behavior.Status getStatus(){return original.getStatus();}
    @Override public Set<MemoryModuleType<?>> getRequiredMemories(){return original.getRequiredMemories();}
    @Override public String debugString(){return "MinerJobGate("+original.debugString()+")";}
    @Override public boolean tryStart(ServerLevel level,PathfinderMob mob,long time){return !(mob instanceof Villager v&&RoleController.namedMiner(v))&&original.tryStart(level,mob,time);}
    @Override public void tickOrStop(ServerLevel level,PathfinderMob mob,long time){if(mob instanceof Villager v&&RoleController.namedMiner(v))original.doStop(level,mob,time);else original.tickOrStop(level,mob,time);}
    @Override public void doStop(ServerLevel level,PathfinderMob mob,long time){original.doStop(level,mob,time);}
}
