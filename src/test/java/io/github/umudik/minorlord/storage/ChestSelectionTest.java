package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChestSelectionTest {
    @Test void literalNamesHaveNoSemanticFoodCategoryOrWildcard() {
        assertTrue(ChestNameFilter.matches("iron_ingot", "iron"));
        assertTrue(ChestNameFilter.matches("iron_ingot", "ingot"));
        assertTrue(ChestNameFilter.matches("custom_iron_ingot_fragment", "iron_ingot"));
        assertTrue(ChestNameFilter.matches("modded_melon_slice", "melon"));
        assertFalse(ChestNameFilter.matches("bread", "food"));
        assertFalse(ChestNameFilter.matches("iron_ingot", "*iron*"));
        assertFalse(ChestNameFilter.matches("iron_ingot", ""));
        assertFalse(ChestNameFilter.matches("iron_ingot", "   "));
    }

    @Test void labelsAreNormalizedTheWayPlayersWriteThem() {
        assertTrue(ChestNameFilter.matches("iron_ingot", "Iron"));
        assertTrue(ChestNameFilter.matches("iron_ingot", "iron ingot"));
        assertTrue(ChestNameFilter.matches("raw_copper", "Raw Copper"));
        assertTrue(ChestNameFilter.matches("raw_copper", "copper"));
        assertTrue(ChestNameFilter.matches("raw_copper", "  RAW-copper "));
        assertFalse(ChestNameFilter.matches("raw_copper", "raw  iron"));
        assertEquals(10, ChestNameFilter.specificity("raw_copper", List.of("Raw Copper", "copper")));
    }

    @Test void specificLabelBeatsCloserBroadLabelAndInvalidTargetsAreExcluded() {
        var choices = List.of(candidate("broad", "leather"), candidate("specific", "leather_boots"),
                candidate("unloaded", "boots"), candidate("unlabelled"));
        var distances = Map.of("broad", 4d, "specific", 20d, "unloaded", 1d, "unlabelled", 0d);
        assertEquals(List.of("specific"), NearestChestTargets.find("leather_boots", choices,
                key -> !key.equals("unloaded"), distances::get));
        assertEquals(List.of("broad"), NearestChestTargets.find("leather", choices,
                key -> !key.equals("unloaded"), distances::get));
    }

    @Test void unavailableSpecificTargetFallsBackToBroaderMatch() {
        var choices = List.of(candidate("broad", "leather"), candidate("full", "leather_boots"));
        assertEquals(List.of("broad"), NearestChestTargets.find("leather_boots", choices,
                key -> !key.equals("full"), key -> 1));
    }

    @Test void specificityIsTheLongestMatchingLabel() {
        assertEquals(13, ChestNameFilter.specificity("leather_boots", List.of("leather", "leather_boots", "boots")));
        assertEquals(7, ChestNameFilter.specificity("leather_boots", List.of("leather", "iron")));
        assertEquals(-1, ChestNameFilter.specificity("leather_boots", List.of("iron", " ")));
    }

    @Test void equallySpecificTiesAreReturnedWithoutInventingPriority() {
        var choices = List.of(candidate("a", "iron"), candidate("b", "iron"), candidate("c", "ingot"));
        assertEquals(List.of("a", "b"), NearestChestTargets.find("iron_nugget", choices, key -> true, key -> 1));
    }

    @Test void missingTargetAndNonfiniteDistancesAreHandled() {
        assertTrue(NearestChestTargets.find("bread", List.of(candidate("a", "food")), key -> true, key -> 1).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> NearestChestTargets.find("iron", List.of(candidate("a", "iron")), key -> true, key -> Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> NearestChestTargets.find("iron", List.of(candidate("a", "iron")), key -> true, key -> -1));
    }

    private static NearestChestTargets.Candidate<String> candidate(String id, String... labels) {
        return new NearestChestTargets.Candidate<>(id, List.of(labels));
    }
}
