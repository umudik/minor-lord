package io.github.umudik.minorlord.storage;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Exact, synchronous, transactional transfers. Never call across ticks or off the owning server thread. */
public final class InventoryTransfers {
    private InventoryTransfers() {}

    /**
     * Identities describe underlying physical inventories, not wrapper instances.
     * A double chest must include both halves; an NPC inventory needs its own stable identity.
     */
    public record Endpoint<T>(Set<?> inventoryIdentities, Storage<T> storage) {
        public Endpoint {
            inventoryIdentities = Set.copyOf(inventoryIdentities);
            if (inventoryIdentities.isEmpty()) {
                throw new IllegalArgumentException("Underlying inventory identity is required");
            }
            Objects.requireNonNull(storage, "storage");
        }
    }

    public enum Result { MOVED, INVALIDATED, INSUFFICIENT_SOURCE_OR_SPACE }

    /**
     * Moves exactly amount or rolls back everything. The variant equality includes item components
     * when called with Fabric ItemVariant, so distinct enchantments/damage are never merged.
     * The caller's guard must check the current source/target permissions, labels and physical arrival;
     * it runs before mutation and again before commit and must be read-only.
     * No fallback, drop or item creation is performed. Storage implementations must honor Fabric's
     * transaction contract; arbitrary external inventories that bypass it are not supported.
     */
    public static <T> Result moveExact(
            Endpoint<T> source, Endpoint<T> target, T variant, long amount,
            BooleanSupplier stillValid) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(stillValid, "stillValid");
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (source.storage() == target.storage()
                || !Collections.disjoint(source.inventoryIdentities(), target.inventoryIdentities())) {
            throw new IllegalArgumentException("Source and target inventories overlap");
        }
        if (!stillValid.getAsBoolean()) {
            return Result.INVALIDATED;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(source.storage(), target.storage(), variant::equals, amount, transaction);
            if (moved != amount) {
                return Result.INSUFFICIENT_SOURCE_OR_SPACE;
            }
            if (!stillValid.getAsBoolean()) {
                return Result.INVALIDATED;
            }
            transaction.commit();
            return Result.MOVED;
        }
    }
}
