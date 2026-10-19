package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;

class InventoryTransfersTest {
    static InventoryTransfers.Endpoint<String> endpoint(Object id, TestStorage storage) {
        return new InventoryTransfers.Endpoint<>(Set.of(id), storage);
    }

    @Test void exactMoveConservesItems() {
        var source = new TestStorage("iron", 12, 64);
        var target = new TestStorage("iron", 3, 64);
        assertEquals(InventoryTransfers.Result.MOVED, InventoryTransfers.moveExact(endpoint("source", source), endpoint("target", target), "iron", 7, () -> true));
        assertEquals(5, source.amount);
        assertEquals(10, target.amount);
    }

    @Test void exactQuantityCanSpanMultipleSourceSlotsWithoutOvercollection() {
        var first = new TestStorage("iron", 2, 64);
        var second = new TestStorage("iron", 6, 64);
        var combined = new CombinedStorage<String, TestStorage>(List.of(first, second));
        var source = new InventoryTransfers.Endpoint<String>(Set.of("first-slot", "second-slot"), combined);
        var target = new TestStorage("", 0, 64);
        assertEquals(InventoryTransfers.Result.MOVED,
                InventoryTransfers.moveExact(source, endpoint("target", target), "iron", 5, () -> true));
        assertEquals(0, first.amount);
        assertEquals(3, second.amount);
        assertEquals(5, target.amount);
    }

    @Test void insufficientSourceAndTargetSpaceRollBack() {
        var source = new TestStorage("iron", 12, 64);
        var target = new TestStorage("iron", 60, 64);
        assertEquals(InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", 5, () -> true));
        assertEquals(12, source.amount);
        assertEquals(60, target.amount);
        target.amount = 0;
        assertEquals(InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", 13, () -> true));
        assertEquals(12, source.amount);
        assertEquals(0, target.amount);
    }

    @Test void currentValidityBeforeAndAfterMutationPreventsStaleTransfers() {
        var source = new TestStorage("iron", 12, 64);
        var target = new TestStorage("", 0, 64);
        assertEquals(InventoryTransfers.Result.INVALIDATED,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", 5, () -> false));
        var checks = new AtomicInteger();
        assertEquals(InventoryTransfers.Result.INVALIDATED,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", 5,
                        () -> checks.incrementAndGet() == 1));
        assertEquals(12, source.amount);
        assertEquals(0, target.amount);
    }

    @Test void exceptionsAfterStorageMutationRollBack() {
        var source = new TestStorage("iron", 12, 64);
        var target = new TestStorage("", 0, 64);
        target.failInsert = true;
        assertThrows(RuntimeException.class, () -> InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", 5, () -> true));
        assertEquals(12, source.amount);
        assertEquals(0, target.amount);
        assertEquals("", target.variant);
    }

    @Test void rejectsNonpositiveRequestsAndAliasedDoubleChestHalves() {
        var source = new TestStorage("iron", 12, 64);
        var target = new TestStorage("", 0, 64);
        var s = endpoint("left-half", source);
        var t = new InventoryTransfers.Endpoint<String>(Set.of("left-half", "right-half"), target);
        assertThrows(IllegalArgumentException.class, () -> InventoryTransfers.moveExact(s, t, "iron", 1, () -> true));
        assertThrows(IllegalArgumentException.class, () -> InventoryTransfers.moveExact(s, endpoint("t", target), "iron", 0, () -> true));
        assertThrows(IllegalArgumentException.class, () -> InventoryTransfers.moveExact(s, endpoint("t", target), "iron", -1, () -> true));
        assertEquals(12, source.amount);
        assertEquals(0, target.amount);
    }

    @Test void distinctResourcesAreNeverSubstitutedAndLongBoundsDoNotOverflow() {
        var source = new TestStorage("book:mending", 1, 1);
        var target = new TestStorage("book:sharpness", 1, 1);
        assertEquals(InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "book:mending", 1, () -> true));
        var empty = new TestStorage("", 0, 64);
        assertEquals(InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE,
                InventoryTransfers.moveExact(endpoint("s", source), endpoint("e", empty), "book:mending", Long.MAX_VALUE, () -> true));
        assertEquals(1, source.amount);
        assertEquals(0, empty.amount);
    }

    @Test void randomizedTransfersPreserveTotalAndBounds() {
        var random = new Random(263);
        for (int iteration = 0; iteration < 1000; iteration++) {
            long beforeSource = random.nextInt(65);
            long beforeTarget = random.nextInt(65);
            long requested = 1 + random.nextInt(80);
            var source = new TestStorage("iron", beforeSource, 64);
            var target = new TestStorage("iron", beforeTarget, 64);
            boolean fits = requested <= beforeSource && requested <= 64 - beforeTarget;
            var result = InventoryTransfers.moveExact(endpoint("s", source), endpoint("t", target), "iron", requested, () -> true);
            assertEquals(fits ? InventoryTransfers.Result.MOVED : InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE, result);
            assertEquals(beforeSource + beforeTarget, source.amount + target.amount);
            assertTrue(source.amount >= 0 && source.amount <= 64 && target.amount >= 0 && target.amount <= 64);
            assertEquals(fits ? beforeSource - requested : beforeSource, source.amount);
        }
    }
}
