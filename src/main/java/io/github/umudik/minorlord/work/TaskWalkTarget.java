package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.roles.WorkGate;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;

/** Uses vanilla WALK_TARGET movement; never teleports or clears a newer vanilla safety target. */
public final class TaskWalkTarget {
    public interface Memory {
        Optional<WalkTarget> get();
        void set(WalkTarget target);
        void erase();
    }

    private WalkTarget issued;
    private java.util.function.BiFunction<Villager,BlockPos,BlockPos> route=(villager,target)->target;
    /** A role that knows the way (a mine entrance) sends its walks along it: route maps a target to the next stop. */
    public TaskWalkTarget via(java.util.function.BiFunction<Villager,BlockPos,BlockPos> route){this.route=route;return this;}

    /** Refuses to steal an existing foreign target, even at the same position; an idle stroll always gives way to work. */
    public boolean offer(Memory memory, WalkTarget target) {
        Objects.requireNonNull(memory);
        Objects.requireNonNull(target);
        if (memory.get().filter(current -> current != issued && !Idle.stroll(current)).isPresent()) return false;
        issued = target;
        memory.set(target);
        return true;
    }

    /** Identity, not coordinate equality: a vanilla replacement at the same position is still foreign. */
    public void release(Memory memory) {
        if (issued != null && memory.get().filter(current -> current == issued).isPresent()) memory.erase();
        issued = null;
    }

    public boolean offer(Villager villager, BlockPos target, float speed, int arrivalRadius) {
        if (!Float.isFinite(speed) || speed <= 0 || arrivalRadius < 0) {
            throw new IllegalArgumentException("Explicit positive speed and nonnegative arrival radius required");
        }
        if (!WorkGate.allows(villager) || !villager.level().hasChunkAt(target)) return false;
        var stop = route.apply(villager, target);
        return offer(memory(villager), new WalkTarget(stop, speed, stop.equals(target) ? arrivalRadius : 1));
    }

    public void release(Villager villager) {
        boolean owned=issued!=null&&villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).filter(t->t==issued).isPresent();
        release(memory(villager));
        if(owned&&WorkGate.allows(villager)){villager.getBrain().eraseMemory(MemoryModuleType.PATH);villager.getNavigation().stop();}
    }

    private static Memory memory(Villager villager) {
        return new Memory() {
            @Override public Optional<WalkTarget> get() { return villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET); }
            @Override public void set(WalkTarget target) { villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, target); }
            @Override public void erase() { villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); }
        };
    }
}
