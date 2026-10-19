package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.production.BatchAllocator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class BatchAllocatorTest {
    @Test void completeBatchesTakeOnlyExactIngredients() {
        var result = BatchAllocator.maximize(List.<Predicate<String>>of("sugar"::equals, "wheat"::equals),
                Map.of("sugar", 100L, "wheat", 3L), 1000);
        assertEquals(3, result.batches());
        assertEquals(Map.of("sugar", 3L, "wheat", 3L), result.withdrawals());
    }

    @Test void overlappingAlternativesDoNotGreedilyStarveARequiredIngredient() {
        var result = BatchAllocator.maximize(List.<Predicate<String>>of(item -> item.equals("oak") || item.equals("birch"), "oak"::equals),
                Map.of("oak", 4L, "birch", 4L), 100);
        assertEquals(4, result.batches());
        assertEquals(Map.of("oak", 4L, "birch", 4L), result.withdrawals());
        assertEquals(Map.of("oak", 4L), result.perIngredient().get(1));
    }

    @Test void missingIngredientDoesNotCollectSpeculativeInputs() {
        var result = BatchAllocator.maximize(List.<Predicate<String>>of("sugar"::equals, "wheat"::equals),
                Map.of("sugar", 100L), 100);
        assertEquals(0, result.batches());
        assertTrue(result.withdrawals().isEmpty());
    }

    @Test void workBudgetIsExplicitAndBoundsDoNotOverflow() {
        var result = BatchAllocator.maximize(List.<Predicate<String>>of("stone"::equals), Map.of("stone", Long.MAX_VALUE), Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, result.batches());
        assertEquals(Long.MAX_VALUE, result.withdrawals().get("stone"));
        assertEquals(2, BatchAllocator.maximize(List.<Predicate<String>>of("stone"::equals), Map.of("stone", 99L), 2).batches());
        assertThrows(IllegalArgumentException.class, () -> BatchAllocator.maximize(List.of(), Map.of("stone", 1L), 1));
    }

    @Test void repeatedSlotsConsumeRepeatedAmountsAndUnrelatedItemsStayUnused() {
        var result = BatchAllocator.maximize(List.<Predicate<String>>of("wheat"::equals, "wheat"::equals, "wheat"::equals),
                Map.of("wheat", 10L, "diamond", 999L), 100);
        assertEquals(3, result.batches());
        assertEquals(Map.of("wheat", 9L), result.withdrawals());
    }

    @Test void randomOverlappingPoolsAgreeWithIndependentSmallBruteForceBound() {
        var random = new Random(263);
        for (int i = 0; i < 500; i++) {
            long oak = random.nextInt(12);
            long birch = random.nextInt(12);
            var result = BatchAllocator.maximize(List.<Predicate<String>>of(item -> item.equals("oak") || item.equals("birch"), "oak"::equals),
                    Map.of("oak", oak, "birch", birch), 100);
            long expected = 0;
            for (long batches = 0; batches <= oak; batches++) {
                if (batches * 2 <= oak + birch) expected = batches;
            }
            assertEquals(expected, result.batches());
            assertTrue(result.withdrawals().getOrDefault("oak", 0L) <= oak);
            assertTrue(result.withdrawals().getOrDefault("birch", 0L) <= birch);
            assertEquals(expected * 2, result.withdrawals().values().stream().mapToLong(Long::longValue).sum());
        }
    }
}
