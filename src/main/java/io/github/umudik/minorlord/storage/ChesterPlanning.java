package io.github.umudik.minorlord.storage;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

/** A read-only first step for Chester. Walking, village registration and event wiring are separate. */
public final class ChesterPlanning {
    private ChesterPlanning() {}

    /**
     * Untagged source inventories are allowed; an already matching source is left alone.
     * No item is extracted by planning. A destination must accept the entire proposed carried amount.
     * Equal-distance results are returned together rather than choosing a gameplay tie rule.
     */
    public static <T> List<InventoryTransfers.Endpoint<T>> findTargets(
            String comparableItemName, T variant, long amount,
            NearestChestTargets.Candidate<InventoryTransfers.Endpoint<T>> source,
            Collection<NearestChestTargets.Candidate<InventoryTransfers.Endpoint<T>>> destinations,
            Predicate<InventoryTransfers.Endpoint<T>> currentlyEligible,
            ToDoubleFunction<InventoryTransfers.Endpoint<T>> distance) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(currentlyEligible, "currentlyEligible");
        if (amount <= 0) {
            throw new IllegalArgumentException("Planned amount must be positive");
        }
        if (source.comparableLabels().stream()
                .anyMatch(label -> ChestNameFilter.matches(comparableItemName, label))) {
            return List.of();
        }
        return NearestChestTargets.find(comparableItemName, destinations, target ->
                java.util.Collections.disjoint(source.target().inventoryIdentities(), target.inventoryIdentities())
                        && source.target().storage() != target.storage()
                        && currentlyEligible.test(target)
                        && StorageUtil.simulateInsert(target.storage(), variant, amount, null) == amount,
                distance);
    }
}
