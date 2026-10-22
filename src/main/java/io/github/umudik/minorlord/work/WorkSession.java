package io.github.umudik.minorlord.work;

import java.util.Objects;

/** Keeps the same task across fear/rest pauses; this controller never owns or deletes item stacks. */
public final class WorkSession<C> {
    public enum State { READY, RUNNING, PAUSED, FINISHED, FAILED }
    /** IDLE: nothing to do right now; the villager may look alive (Idle) until work turns up. */
    public enum Step { CONTINUE, IDLE, FINISHED }

    public interface Task<C> {
        Step step(C context);
        /** Release only task-owned movement; do not clear vanilla fear/sleep memories or discard items. */
        void pause(C context);
        default void finish(C context) { pause(context); }
    }

    private final Task<C> task;
    private State state = State.READY;

    public WorkSession(Task<C> task) { this.task = Objects.requireNonNull(task); }
    public State state() { return state; }

    public void tick(C context, boolean vanillaAllowsWork) {
        if (state == State.FINISHED || state == State.FAILED) return;
        if (!vanillaAllowsWork) {
            pause(context);
            return;
        }
        state = State.RUNNING;
        try {
            if (Objects.requireNonNull(task.step(context), "Task step result") == Step.FINISHED) {
                state = State.FINISHED;
                task.finish(context);
            }
        } catch (RuntimeException | Error failure) {
            state = State.FAILED;
            // Preserve the same task and all external inventories; do not auto-retry/drop/reset.
            try { task.pause(context); } catch (RuntimeException | Error pauseFailure) { failure.addSuppressed(pauseFailure); }
            throw failure;
        }
    }

    public void pause(C context) {
        if (state == State.FINISHED || state == State.FAILED || state == State.PAUSED) return;
        state = State.PAUSED;
        try {
            task.pause(context);
        } catch (RuntimeException | Error failure) {
            state = State.FAILED;
            throw failure;
        }
    }
}
