package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.roles.ManagedVillagers;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.work.ManagedWorkSessions;
import io.github.umudik.minorlord.work.WorkSession;
import java.util.UUID;
import net.minecraft.world.entity.npc.villager.Villager;
import org.junit.jupiter.api.Test;

class ManagedWorkBoundaryTest {
    @Test void tasksRequireManagedMatchingRolesAndCannotOverwriteUnreturnedTasks() {
        var members = new ManagedVillagers();
        var work = new ManagedWorkSessions(members);
        var id = UUID.randomUUID();
        var task = new Task();
        assertThrows(IllegalArgumentException.class, () -> work.install(id, Role.CHESTER, task));
        members.assign(id, new ManagedVillagers.Assignment(UUID.randomUUID(), Role.CHESTER));
        assertThrows(IllegalArgumentException.class, () -> work.install(id, Role.MINER, task));
        work.install(id, Role.CHESTER, task);
        assertTrue(work.find(id).isPresent());
        assertThrows(IllegalStateException.class, () -> work.install(id, Role.CHESTER, new Task()));
    }

    @Test void changedRolesCannotRunAnOldTaskButItsPauseBoundaryIsRetained() {
        var members = new ManagedVillagers();
        var work = new ManagedWorkSessions(members);
        var id = UUID.randomUUID();
        var village = UUID.randomUUID();
        var task = new Task();
        members.assign(id, new ManagedVillagers.Assignment(village, Role.CHESTER));
        work.install(id, Role.CHESTER, task);
        members.assign(id, new ManagedVillagers.Assignment(village, Role.MINER));
        assertTrue(work.find(id).isEmpty());
        work.pauseRetained(id, null);
        assertEquals(1, task.pauses);
        assertEquals(0, task.steps);
    }

    @Test void movingToAnotherVillageInvalidatesTheOldScopeEvenForTheSameRole() {
        var members = new ManagedVillagers();
        var work = new ManagedWorkSessions(members);
        var id = UUID.randomUUID();
        members.assign(id, new ManagedVillagers.Assignment(UUID.randomUUID(), Role.CHESTER));
        work.install(id, Role.CHESTER, new Task());
        members.assign(id, new ManagedVillagers.Assignment(UUID.randomUUID(), Role.CHESTER));
        assertTrue(work.find(id).isEmpty());
    }

    private static final class Task implements WorkSession.Task<Villager> {
        int steps;
        int pauses;
        @Override public WorkSession.Step step(Villager ignored) { steps++; return WorkSession.Step.CONTINUE; }
        @Override public void pause(Villager ignored) { pauses++; }
    }
}
