package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.work.TaskWalkTarget;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.junit.jupiter.api.Test;

class TaskWalkTargetTest {
    private static final class Memory implements TaskWalkTarget.Memory {
        WalkTarget target;
        @Override public Optional<WalkTarget> get() { return Optional.ofNullable(target); }
        @Override public void set(WalkTarget next) { target = next; }
        @Override public void erase() { target = null; }
    }

    @Test void releasesOnlyItsOwnWalkTarget() {
        var owner = new TaskWalkTarget();
        var memory = new Memory();
        assertTrue(owner.offer(memory, target()));
        owner.release(memory);
        assertNull(memory.target);
    }

    @Test void aVanillaReplacementEvenAtSameCoordinatesSurvivesPause() {
        var owner = new TaskWalkTarget();
        var memory = new Memory();
        assertTrue(owner.offer(memory, target()));
        var escapeTarget = target();
        memory.set(escapeTarget);
        owner.release(memory);
        assertSame(escapeTarget, memory.target);
    }

    @Test void doesNotStealAnExistingForeignTarget() {
        var owner = new TaskWalkTarget();
        var memory = new Memory();
        var vanillaTarget = target();
        memory.set(vanillaTarget);
        assertFalse(owner.offer(memory, target()));
        assertSame(vanillaTarget, memory.target);
    }

    @Test void canUpdateOwnTargetWithoutDiscardingNewForeignTargets() {
        var owner = new TaskWalkTarget();
        var memory = new Memory();
        assertTrue(owner.offer(memory, target()));
        var next = target();
        assertTrue(owner.offer(memory, next));
        assertSame(next, memory.target);
    }

    private static WalkTarget target() { return new WalkTarget(BlockPos.ZERO, 0.5F, 1); }
}
