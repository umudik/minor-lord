package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.roles.ManagedVillagers;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.roles.WorkGate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;

/** Explicit task installation only. No role/name precedence, walking or source policy is inferred. */
public final class ManagedWorkSessions {
    private record Bound(ManagedVillagers.Assignment assignment, WorkSession<Villager> session) {}
    private final ManagedVillagers membership;
    private final Map<UUID, Bound> sessions = new HashMap<>();

    public ManagedWorkSessions(ManagedVillagers membership) { this.membership = membership; }

    public void install(UUID villagerId, Role expectedRole, WorkSession.Task<Villager> task) {
        var assignment = membership.find(villagerId);
        if (assignment.map(value -> value.role() != expectedRole).orElse(true)) {
            throw new IllegalArgumentException("Task must match an explicitly managed role");
        }
        if (sessions.containsKey(villagerId)) {
            throw new IllegalStateException("Existing task must be handed back safely before replacement");
        }
        sessions.put(villagerId, new Bound(assignment.orElseThrow(), new WorkSession<>(task)));
    }

    /** Refreshes only this managed entity's brain so its existing vanilla schedule gets the WORK hook. */
    public void installFor(Villager villager, Role expectedRole, WorkSession.Task<Villager> task) {
        if (!(villager.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Managed tasks may only be installed on the server");
        }
        if (!WorkGate.allows(villager)) {
            throw new IllegalStateException("Defer brain refresh until vanilla work is safely active");
        }
        install(villager.getUUID(), expectedRole, task);
        villager.refreshBrain(level);
    }

    public Optional<WorkSession<Villager>> find(UUID id) {
        Bound bound = sessions.get(id);
        if (bound == null || membership.find(id).filter(bound.assignment()::equals).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(bound.session());
    }

    public void pauseRetained(UUID id, Villager context) {
        Bound bound = sessions.get(id);
        if (bound != null) bound.session().pause(context);
    }
    public void removeFor(Villager villager){
        var bound=sessions.remove(villager.getUUID());if(bound!=null)bound.session().pause(villager);
    }

    /** The owner must persist any task data before server shutdown; entity inventories stay external. */
    public void clear() { sessions.clear(); }
}
