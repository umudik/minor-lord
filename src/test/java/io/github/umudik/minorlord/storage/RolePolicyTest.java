package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import io.github.umudik.minorlord.roles.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RolePolicyTest {
    @Test void exactApprovedThresholdsPreserveStackUnitsAndUnlimitedCook() {
        for (var role : List.of(Role.FLETCHER, Role.WEAPONSMITH, Role.TOOLSMITH, Role.ARMORER,
                Role.LEATHERWORKER, Role.CARTOGRAPHER)) {
            assertEquals(5, StockLimits.threshold(role, 64).orElseThrow());
            assertTrue(StockLimits.shouldStart(role, 4, 64));
            assertFalse(StockLimits.shouldStart(role, 5, 64));
        }
        assertEquals(320, StockLimits.threshold(Role.MASON, 64).orElseThrow());
        assertEquals(80, StockLimits.threshold(Role.MASON, 16).orElseThrow());
        assertTrue(StockLimits.shouldStart(Role.MASON, 79, 16));
        assertFalse(StockLimits.shouldStart(Role.MASON, 80, 16));
        assertEquals(3, StockLimits.room(Role.TOOLSMITH, 2, 1).orElseThrow());
        assertEquals(0, StockLimits.room(Role.TOOLSMITH, 9, 1).orElseThrow());
        assertTrue(StockLimits.room(Role.COOK, 100, 64).isEmpty());
        assertTrue(StockLimits.threshold(Role.COOK, 64).isEmpty());
        assertTrue(StockLimits.shouldStart(Role.COOK, Long.MAX_VALUE, 64));
        assertTrue(StockLimits.threshold(Role.CLERIC, 1).isEmpty());
        assertTrue(StockLimits.threshold(Role.LIBRARIAN, 1).isEmpty());
    }

    @Test void invalidStockInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> StockLimits.threshold(Role.MASON, 0));
        assertThrows(IllegalArgumentException.class, () -> StockLimits.shouldStart(Role.FLETCHER, -1, 64));
    }

    @Test void vanillaWorkMayNotOverrideFearRestOrSleep() {
        assertTrue(WorkGate.allows(new WorkGate.State(true, true, true, false, false, false, false)));
        assertFalse(WorkGate.allows(new WorkGate.State(true, true, true, true, false, false, false)));
        assertFalse(WorkGate.allows(new WorkGate.State(true, true, true, false, true, false, false)));
        assertFalse(WorkGate.allows(new WorkGate.State(true, true, true, false, false, true, false)));
        assertFalse(WorkGate.allows(new WorkGate.State(true, true, true, false, false, false, true)));
        assertFalse(WorkGate.allows(new WorkGate.State(true, true, false, false, false, false, false)));
        assertFalse(WorkGate.allows(new WorkGate.State(false, true, true, false, false, false, false)));
    }

    @Test void membershipDoesNotImplicitlyManageOtherVillagers() {
        var registry = new ManagedVillagers();
        var managed = UUID.randomUUID();
        var other = UUID.randomUUID();
        assertTrue(registry.find(managed).isEmpty());
        registry.assign(managed, new ManagedVillagers.Assignment(UUID.randomUUID(), Role.CHESTER));
        assertEquals(Role.CHESTER, registry.find(managed).orElseThrow().role());
        assertTrue(registry.find(other).isEmpty());
        registry.clear();
        assertTrue(registry.find(managed).isEmpty());
    }
}
