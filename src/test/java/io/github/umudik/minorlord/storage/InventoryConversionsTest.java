package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.production.InventoryConversions;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import org.junit.jupiter.api.Test;

class InventoryConversionsTest {
    @Test void exactInputsAreConsumedOnlyWhenAllOutputsFit() {
        var books = new TestStorage("book", 3, 64);
        var output = new TestStorage("", 0, 1);
        var result = InventoryConversions.convertExact(InventoryTransfersTest.endpoint("s", books),
                InventoryTransfersTest.endpoint("t", output), Map.of("book", 1L), Map.of("enchanted:level1", 1L), () -> true);
        assertEquals(InventoryConversions.Result.PRODUCED, result);
        assertEquals(2, books.amount);
        assertEquals(1, output.amount);
        assertEquals("enchanted:level1", output.variant);
    }

    @Test void fullOutputRollsBackConsumedInputs() {
        var books = new TestStorage("book", 3, 64);
        var output = new TestStorage("other", 1, 1);
        assertEquals(InventoryConversions.Result.OUTPUT_FULL, InventoryConversions.convertExact(
                InventoryTransfersTest.endpoint("s", books), InventoryTransfersTest.endpoint("t", output),
                Map.of("book", 1L), Map.of("enchanted", 1L), () -> true));
        assertEquals(3, books.amount);
        assertEquals("other", output.variant);
    }

    @Test void shortageOfLaterIngredientRestoresEarlierConsumption() {
        var sugar = new TestStorage("sugar", 10, 64);
        var flour = new TestStorage("wheat", 0, 64);
        var combined = new CombinedStorage<String, TestStorage>(List.of(sugar, flour));
        var source = new InventoryTransfers.Endpoint<String>(Set.of("sugar", "flour"), combined);
        var output = new TestStorage("", 0, 64);
        assertEquals(InventoryConversions.Result.INPUT_SHORTAGE, InventoryConversions.convertExact(source,
                InventoryTransfersTest.endpoint("output", output), Map.of("sugar", 1L, "wheat", 1L),
                Map.of("product", 1L), () -> true));
        assertEquals(10, sugar.amount);
        assertEquals(0, output.amount);
    }

    @Test void invalidationAfterConversionRestoresInputsAndOutput() {
        var source = new TestStorage("book", 1, 64);
        var output = new TestStorage("", 0, 64);
        var checks = new AtomicInteger();
        assertEquals(InventoryConversions.Result.INVALIDATED, InventoryConversions.convertExact(
                InventoryTransfersTest.endpoint("s", source), InventoryTransfersTest.endpoint("t", output),
                Map.of("book", 1L), Map.of("enchanted", 1L), () -> checks.incrementAndGet() == 1));
        assertEquals(1, source.amount);
        assertEquals(0, output.amount);
    }

    @Test void emptyOrNonpositiveRecipesCannotCreateFreeItems() {
        var source = InventoryTransfersTest.endpoint("s", new TestStorage("book", 1, 64));
        var target = InventoryTransfersTest.endpoint("t", new TestStorage("", 0, 64));
        assertThrows(IllegalArgumentException.class, () -> InventoryConversions.convertExact(source, target, Map.of(), Map.of("product", 1L), () -> true));
        assertThrows(IllegalArgumentException.class, () -> InventoryConversions.convertExact(source, target, Map.of("book", 0L), Map.of("product", 1L), () -> true));
    }
}
