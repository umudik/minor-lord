package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.mining.MiningBounds;
import io.github.umudik.minorlord.roles.ButcherPolicy;
import org.junit.jupiter.api.Test;

class MiningAndAnimalPolicyTest {
    @Test void radiusIsMeasuredFromSignNotEntranceAndBottomIsOneBelow() {
        var bounds = new MiningBounds(100, -53, 100, 1000);
        assertEquals(-54, bounds.bottomY());
        assertTrue(bounds.containsHorizontal(1100, 100));
        assertTrue(bounds.containsHorizontal(-900, 100));
        assertFalse(bounds.containsHorizontal(1101, 100));
        assertFalse(bounds.containsHorizontal(1100, 1100));
    }

    @Test void radiusMathRejectsInvalidInputAndDoesNotWrapWorldCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new MiningBounds(0, 0, 0, 0));
        var bounds = new MiningBounds(Integer.MAX_VALUE, 1, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertFalse(bounds.containsHorizontal(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertTrue(bounds.containsHorizontal(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test void butcherAlwaysLeavesEightAdultsAndOnlyUsesShearedSheep() {
        assertFalse(ButcherPolicy.mayCull(8, true, false, false));
        assertTrue(ButcherPolicy.mayCull(9, true, false, false));
        assertFalse(ButcherPolicy.mayCull(20, false, false, false));
        assertFalse(ButcherPolicy.mayCull(20, true, true, false));
        assertTrue(ButcherPolicy.mayCull(9, true, true, true));
        assertFalse(ButcherPolicy.mayCull(8, true, true, true));
    }
}
