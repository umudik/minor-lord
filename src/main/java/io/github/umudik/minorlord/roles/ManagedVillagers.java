package io.github.umudik.minorlord.roles;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Explicit village-owned membership. Call only on the server thread; no global villager matching. */
public final class ManagedVillagers {
    public record Assignment(UUID villageId, Role role) {
        public Assignment {
            Objects.requireNonNull(villageId, "villageId");
            Objects.requireNonNull(role, "role");
        }
    }

    private final Map<UUID, Assignment> assignments = new HashMap<>();

    public Optional<Assignment> find(UUID villagerId) {
        return Optional.ofNullable(assignments.get(villagerId));
    }

    public void assign(UUID villagerId, Assignment assignment) {
        assignments.put(Objects.requireNonNull(villagerId), Objects.requireNonNull(assignment));
    }

    /** Loaded members of one village with their roles. */
    public Map<UUID, Role> members(UUID villageId) {
        Map<UUID, Role> members = new HashMap<>();
        assignments.forEach((id, a) -> { if (a.villageId().equals(villageId)) members.put(id, a.role()); });
        return members;
    }

    public void remove(UUID villagerId) { assignments.remove(villagerId); }
    public void clear() { assignments.clear(); }
}
