package io.github.umudik.minorlord.storage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Count only the caller's authorized registered/labelled chest snapshots, never NPC reservations. */
public final class ChestStock {
    private ChestStock() {}

    public static <T> long countExact(T resource, Collection<InventoryTransfers.Endpoint<T>> chests) {
        Objects.requireNonNull(resource, "resource");
        return countMatching(resource::equals, chests);
    }

    /** Lets the role choose registry-item identity versus exact components without guessing policy. */
    public static <T> long countMatching(Predicate<T> resourceMatches, Collection<InventoryTransfers.Endpoint<T>> chests) {
        Objects.requireNonNull(resourceMatches, "resourceMatches");
        Set<Object> seen = new HashSet<>();
        long count = 0;
        for (var chest : chests) {
            for (Object id : chest.inventoryIdentities()) {
                if (!seen.add(id)) throw new IllegalArgumentException("Overlapping chest inventories in stock query");
            }
            for (var view : chest.storage()) {
                if (!view.isResourceBlank() && resourceMatches.test(view.getResource())) {
                    if (view.getAmount() < 0) throw new IllegalStateException("Negative storage amount");
                    count = Math.addExact(count, view.getAmount());
                }
            }
        }
        return count;
    }
}
