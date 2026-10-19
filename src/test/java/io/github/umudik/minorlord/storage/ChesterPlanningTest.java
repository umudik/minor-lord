package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChesterPlanningTest {
    @Test void unlabelledSourcesCanPlanButNoExtractionOccurs() {
        var sourceStorage = new TestStorage("iron", 8, 64);
        var source = candidate("source", sourceStorage);
        var targetStorage = new TestStorage("", 0, 64);
        var target = candidate("target", targetStorage, "iron");
        assertEquals(List.of(target.target()), ChesterPlanning.findTargets("iron", "iron", 8, source, List.of(target), endpoint -> true, endpoint -> 1));
        assertEquals(8, sourceStorage.amount);
        assertEquals(0, targetStorage.amount);
    }

    @Test void alreadyCorrectItemIsLeftInPlace() {
        var source = candidate("source", new TestStorage("iron", 8, 64), "iron");
        var target = candidate("target", new TestStorage("", 0, 64), "iron");
        assertTrue(ChesterPlanning.findTargets("iron", "iron", 8, source, List.of(target), endpoint -> true, endpoint -> 1).isEmpty());
    }

    @Test void fullOrMissingOrInaccessibleTargetsDoNotRemoveSourceItems() {
        var sourceStorage = new TestStorage("iron", 8, 64);
        var source = candidate("source", sourceStorage);
        var full = candidate("full", new TestStorage("iron", 64, 64), "iron");
        var wrong = candidate("wrong", new TestStorage("", 0, 64), "melon");
        assertTrue(ChesterPlanning.findTargets("iron", "iron", 8, source, List.of(full, wrong), endpoint -> true, endpoint -> 1).isEmpty());
        var unreachable = candidate("unreachable", new TestStorage("", 0, 64), "iron");
        assertTrue(ChesterPlanning.findTargets("iron", "iron", 8, source, List.of(unreachable), endpoint -> false, endpoint -> 1).isEmpty());
        assertEquals(8, sourceStorage.amount);
    }

    private static NearestChestTargets.Candidate<InventoryTransfers.Endpoint<String>> candidate(
            String id, TestStorage storage, String... labels) {
        return new NearestChestTargets.Candidate<>(InventoryTransfersTest.endpoint(id, storage), List.of(labels));
    }
}
