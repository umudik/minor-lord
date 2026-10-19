package io.github.umudik.minorlord;
import io.github.umudik.minorlord.storage.InventoryAdditions;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryAdditionsTest {
    @Test void onlyAnActualResourceIncreaseIsAnAddition(){
        assertFalse(InventoryAdditions.added(Map.of("iron",12L),Map.of("iron",12L)));
        assertFalse(InventoryAdditions.added(Map.of("iron",12L),Map.of("iron",4L)));
        assertFalse(InventoryAdditions.added(Map.of("iron",12L),Map.of()));
        assertTrue(InventoryAdditions.added(Map.of("iron",12L),Map.of("iron",13L)));
        assertTrue(InventoryAdditions.added(Map.of("iron",12L),Map.of("iron",4L,"gold",1L)));
    }
    @Test void takingFromAFullChestIsARemovalButRearrangingIsNot(){
        assertTrue(InventoryAdditions.removed(Map.of("iron",64L),Map.of("iron",63L)));
        assertTrue(InventoryAdditions.removed(Map.of("iron",64L),Map.of()));
        assertFalse(InventoryAdditions.removed(Map.of("iron",64L),Map.of("iron",64L)));
        assertFalse(InventoryAdditions.removed(Map.of("iron",12L),Map.of("iron",13L)));
    }
}
