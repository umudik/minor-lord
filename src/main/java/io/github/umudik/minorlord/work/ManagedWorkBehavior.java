package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.WorkGate;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs inside vanilla WORK only; vanilla brain safety activities and WorkGate can stop it. */
public final class ManagedWorkBehavior implements BehaviorControl<Villager> {
    private static final Logger LOGGER = LoggerFactory.getLogger("minor_lord");
    private Behavior.Status status = Behavior.Status.STOPPED;

    @Override public Behavior.Status getStatus() { return status; }
    @Override public Set<MemoryModuleType<?>> getRequiredMemories() { return Set.of(); }
    @Override public String debugString() { return "MinorLordManagedWork"; }

    @Override public boolean tryStart(ServerLevel level, Villager villager, long time) {
        var session = MinorLord.WORK_SESSIONS.find(villager.getUUID());
        if (session.isEmpty() || !WorkGate.allows(villager)
                || session.get().state() == WorkSession.State.FINISHED
                || session.get().state() == WorkSession.State.FAILED) return false;
        status = Behavior.Status.RUNNING;
        return true;
    }

    @Override public void tickOrStop(ServerLevel level, Villager villager, long time) {
        var session = MinorLord.WORK_SESSIONS.find(villager.getUUID());
        if (session.isEmpty() || !WorkGate.allows(villager)) {
            doStop(level, villager, time);
            return;
        }
        try {
            session.get().tick(villager, true);
        } catch (RuntimeException failure) {
            status = Behavior.Status.STOPPED;
            LOGGER.error("Paused failed managed task for villager {}; retained state requires review", villager.getUUID(), failure);
            return;
        }
        if (session.get().state() == WorkSession.State.FINISHED) status = Behavior.Status.STOPPED;
    }

    @Override public void doStop(ServerLevel level, Villager villager, long time) {
        status = Behavior.Status.STOPPED;
        try {
            MinorLord.WORK_SESSIONS.pauseRetained(villager.getUUID(), villager);
        } catch (RuntimeException failure) {
            LOGGER.error("Managed task pause failed for villager {}; retained state requires review", villager.getUUID(), failure);
        }
    }
}
