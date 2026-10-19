package io.github.umudik.minorlord.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Event-indexed work invalidation; no full villager scan on every inventory change. */
public final class InventoryWorkQueue {
    private final Map<Object, Set<UUID>> dependents = new HashMap<>();
    private final LinkedHashSet<UUID> pending = new LinkedHashSet<>();

    public void watch(Object inventoryId, UUID villagerId) {
        dependents.computeIfAbsent(Objects.requireNonNull(inventoryId), key -> new LinkedHashSet<>())
                .add(Objects.requireNonNull(villagerId));
    }

    public void unwatch(UUID villagerId) {
        dependents.values().forEach(watchers -> watchers.remove(villagerId));
        dependents.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        pending.remove(villagerId);
    }

    public void changed(Object inventoryId) {
        pending.addAll(dependents.getOrDefault(inventoryId, Set.of()));
    }

    public List<UUID> drain(int budget) {
        if (budget < 0) throw new IllegalArgumentException("Negative work budget");
        List<UUID> result = new ArrayList<>();
        var iterator = pending.iterator();
        while (result.size() < budget && iterator.hasNext()) {
            result.add(iterator.next());
            iterator.remove();
        }
        return List.copyOf(result);
    }
}
