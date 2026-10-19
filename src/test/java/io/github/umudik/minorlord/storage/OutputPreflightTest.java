package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.production.OutputPreflight;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OutputPreflightTest {
    @Test void jointSimulationDoesNotOverpromiseSharedCapacityAndNeverMutatesTargets() {
        var oneSlot = new TestStorage("", 0, 64);
        var endpoint = InventoryTransfersTest.endpoint("one-slot", oneSlot);
        assertEquals(OutputPreflight.Result.FULL,
                OutputPreflight.check(Map.of("cake", 1L, "bucket", 3L), item -> Optional.of(endpoint)));
        assertEquals(0, oneSlot.amount);
        assertEquals("", oneSlot.variant);
    }

    @Test void missingLaterTargetRollsBackEarlierSimulatedInsertion() {
        var slot = new TestStorage("", 0, 64);
        var endpoint = InventoryTransfersTest.endpoint("slot", slot);
        assertEquals(OutputPreflight.Result.MISSING_TARGET,
                OutputPreflight.check(Map.of("cake", 1L, "bucket", 3L), item -> item.equals("cake") ? Optional.of(endpoint) : Optional.empty()));
        assertEquals(0, slot.amount);
    }
}
