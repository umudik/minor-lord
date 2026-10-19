package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.work.WorkSession;
import org.junit.jupiter.api.Test;

class WorkSessionTest {
    private static final class Task implements WorkSession.Task<Void> {
        int steps;
        int pauses;
        int carried = 7;
        boolean complete;
        boolean fail;
        @Override public WorkSession.Step step(Void context) {
            steps++;
            if (fail) throw new IllegalStateException("Expected task failure");
            return complete ? WorkSession.Step.FINISHED : WorkSession.Step.CONTINUE;
        }
        @Override public void pause(Void context) { pauses++; }
    }

    @Test void fearOrSleepPausesWithoutSteppingOrDiscardingCarriedState() {
        var task = new Task();
        var session = new WorkSession<>(task);
        session.tick(null, true);
        session.tick(null, false);
        session.tick(null, false);
        assertEquals(1, task.steps);
        assertEquals(1, task.pauses);
        assertEquals(7, task.carried);
        assertEquals(WorkSession.State.PAUSED, session.state());
        session.tick(null, true);
        assertEquals(2, task.steps);
        assertEquals(7, task.carried);
    }

    @Test void completeTasksCannotAccidentallyRunTwiceAfterResume() {
        var task = new Task();
        task.complete = true;
        var session = new WorkSession<>(task);
        session.tick(null, true);
        session.tick(null, false);
        session.tick(null, true);
        assertEquals(1, task.steps);
        assertEquals(WorkSession.State.FINISHED, session.state());
        assertEquals(1, task.pauses);
    }

    @Test void exceptionsPauseTheSameTaskWithoutDiscardingState() {
        var task = new Task();
        task.fail = true;
        var session = new WorkSession<>(task);
        assertThrows(IllegalStateException.class, () -> session.tick(null, true));
        assertEquals(WorkSession.State.FAILED, session.state());
        assertEquals(7, task.carried);
        assertEquals(1, task.pauses);
        session.tick(null, true);
        assertEquals(1, task.steps);
    }

    @Test void aFailingPauseCannotAutomaticallyResumeWork() {
        var session = new WorkSession<Void>(new WorkSession.Task<>() {
            @Override public WorkSession.Step step(Void context) { fail("Failed task must not resume"); return WorkSession.Step.CONTINUE; }
            @Override public void pause(Void context) { throw new IllegalStateException("Expected pause failure"); }
        });
        assertThrows(IllegalStateException.class, () -> session.tick(null, false));
        assertEquals(WorkSession.State.FAILED, session.state());
        assertDoesNotThrow(() -> session.tick(null, true));
    }
}
