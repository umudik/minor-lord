package io.github.umudik.minorlord.production;

import io.github.umudik.minorlord.storage.InventoryTransfers;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Atomic last-step consumption/output for a validated recipe, not remote material gathering. */
public final class InventoryConversions {
    /** In-place variant for a persistent actor inventory, with rollback of both consumption and production. */
    public static <T> Result convertInPlace(InventoryTransfers.Endpoint<T> inventory,Map<T,Long> inputs,Map<T,Long> products,BooleanSupplier valid){
        if(!valid.getAsBoolean())return Result.INVALIDATED;
        if(inputs.isEmpty()||products.isEmpty())throw new IllegalArgumentException("Inputs and products required");
        try(var tx=Transaction.openOuter()){
            for(var e:inputs.entrySet()){
                if(e.getValue()<=0)throw new IllegalArgumentException("Positive amount required");
                if(inventory.storage().extract(e.getKey(),e.getValue(),tx)!=e.getValue())return Result.INPUT_SHORTAGE;
            }
            for(var e:products.entrySet()){
                if(e.getValue()<=0)throw new IllegalArgumentException("Positive amount required");
                if(inventory.storage().insert(e.getKey(),e.getValue(),tx)!=e.getValue())return Result.OUTPUT_FULL;
            }
            if(!valid.getAsBoolean())return Result.INVALIDATED;tx.commit();return Result.PRODUCED;
        }
    }
    private InventoryConversions() {}

    public enum Result { PRODUCED, INVALIDATED, INPUT_SHORTAGE, OUTPUT_FULL }

    /**
     * Call only after physical arrival on the server thread. The caller supplies an actual validated
     * recipe and its complete outputs, including any required remainders. This never infers recipes,
     * discards leftovers or replaces a world machine's timing, fuel or access rules.
     */
    public static <T> Result convertExact(
            InventoryTransfers.Endpoint<T> carriedInputs, InventoryTransfers.Endpoint<T> output,
            Map<T, Long> required, Map<T, Long> produced, BooleanSupplier stillValid) {
        Objects.requireNonNull(carriedInputs);
        Objects.requireNonNull(output);
        Objects.requireNonNull(stillValid);
        Map<T, Long> inputs = positiveCopy(required);
        Map<T, Long> outputs = positiveCopy(produced);
        if (carriedInputs.storage() != output.storage()
                && !Collections.disjoint(carriedInputs.inventoryIdentities(), output.inventoryIdentities())) {
            throw new IllegalArgumentException("Aliased physical inventories with different wrappers");
        }
        if (!stillValid.getAsBoolean()) return Result.INVALIDATED;
        try (Transaction transaction = Transaction.openOuter()) {
            for (var input : inputs.entrySet()) {
                if (carriedInputs.storage().extract(input.getKey(), input.getValue(), transaction) != input.getValue()) {
                    return Result.INPUT_SHORTAGE;
                }
            }
            for (var product : outputs.entrySet()) {
                if (output.storage().insert(product.getKey(), product.getValue(), transaction) != product.getValue()) {
                    return Result.OUTPUT_FULL;
                }
            }
            if (!stillValid.getAsBoolean()) return Result.INVALIDATED;
            transaction.commit();
            return Result.PRODUCED;
        }
    }

    private static <T> Map<T, Long> positiveCopy(Map<T, Long> amounts) {
        Map<T, Long> copy = Map.copyOf(amounts);
        if (copy.isEmpty() || copy.values().stream().anyMatch(amount -> amount <= 0)) {
            throw new IllegalArgumentException("Explicit positive inputs and complete outputs required");
        }
        return copy;
    }
}
