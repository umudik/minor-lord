package io.github.umudik.minorlord.production;

import io.github.umudik.minorlord.storage.InventoryTransfers;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Joint output-capacity simulation only. Nothing is reserved while the NPC walks. */
public final class OutputPreflight {
    private OutputPreflight() {}
    public enum Result { READY, MISSING_TARGET, FULL }

    public static <T> Result check(Map<T, Long> products,
            Function<T, Optional<InventoryTransfers.Endpoint<T>>> currentDestination) {
        Map<Object, Storage<T>> physical = new HashMap<>();
        try (Transaction simulation = Transaction.openOuter()) {
            for (var product : products.entrySet()) {
                if (product.getValue() <= 0) throw new IllegalArgumentException("Positive product amount required");
                var target = currentDestination.apply(product.getKey());
                if (target.isEmpty()) return Result.MISSING_TARGET;
                for (Object identity : target.get().inventoryIdentities()) {
                    var previous = physical.putIfAbsent(identity, target.get().storage());
                    if (previous != null && previous != target.get().storage()) {
                        throw new IllegalArgumentException("Output targets must use one wrapper per physical inventory");
                    }
                }
                if (target.get().storage().insert(product.getKey(), product.getValue(), simulation) != product.getValue()) {
                    return Result.FULL;
                }
            }
            return Result.READY;
        } // No commit: all simulated insertions are rolled back, including successful earlier outputs.
    }
}
