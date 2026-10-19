package io.github.umudik.minorlord.storage;

import io.github.umudik.minorlord.village.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VillageMarkerTest {
    @Test void twentyFiveMeansExactlyFiftyByFiftyWithApprovedHalfOpenEdges() {
        var bounds = VillageMarker.parse(List.of("village", "25x25"), 10,-10).orElseThrow();
        assertEquals(new VillageBounds(-15,35,-35,15), bounds);
        int count=0;
        for (int x=-20;x<40;x++) for(int z=-40;z<20;z++) if(bounds.contains(x,z)) count++;
        assertEquals(2500,count);
        assertTrue(bounds.contains(-15,-35)); assertTrue(bounds.contains(34,14));
        assertFalse(bounds.contains(35,14)); assertFalse(bounds.contains(34,15));
    }
    @Test void invalidSizesAndWrongMarkersNeverActivate() {
        for(String size: List.of("0x25","-1x25","25","25x25oops","2147483648x25","1.5x2"))
            assertTrue(VillageMarker.parse(List.of("village",size),0,0).isEmpty());
        assertTrue(VillageMarker.parse(List.of("Mine","25x25"),0,0).isEmpty()); assertEquals(VillageMarker.parse(List.of("village","25x25"),0,0),VillageMarker.parse(List.of("Village","25x25"),0,0)); assertEquals(VillageMarker.parse(List.of("village","25x25"),0,0),VillageMarker.parse(List.of("VILLAGE","25x25"),0,0));
    }
    @Test void longArithmeticDoesNotWrapAtIntegerEdges() {
        var b=VillageMarker.parse(List.of("village","2147483647x1"),Integer.MAX_VALUE,0).orElseThrow();
        assertEquals(4294967294L,b.maxXExclusive()); assertEquals(0,b.minX());
    }
    @Test void conflictingSidesDoNotChooseAnArbitraryVillage() {
        var empty=List.of("",""); var first=List.of("village","25x25");
        assertTrue(VillageMarker.resolveSides(first,List.of("village","30x25"),0,0).isEmpty());
        assertEquals(VillageMarker.parse(first,0,0),VillageMarker.resolveSides(empty,first,0,0));
        assertEquals(VillageMarker.parse(first,0,0),VillageMarker.resolveSides(first,first,0,0));
    }
    @Test void onlyOverlappingPositionsAreInactiveAndDimensionsStaySeparate() {
        var scopes=new VillageScopes<String,String>();
        scopes.update("a","overworld",Optional.of(VillageBounds.centered(0,0,25,25)));
        scopes.update("b","overworld",Optional.of(VillageBounds.centered(40,0,25,25)));
        assertTrue(scopes.active("a").isPresent());
        assertEquals(Optional.of("a"),scopes.at("overworld",0,0));
        assertTrue(scopes.at("overworld",20,0).isEmpty());
        assertTrue(scopes.contains("overworld",20,0));
        assertFalse(scopes.contains("overworld",1000,0));
        assertFalse(scopes.contains("end",20,0));
        scopes.update("c","nether",Optional.of(VillageBounds.centered(0,0,25,25)));
        assertEquals(Optional.of("c"),scopes.at("nether",0,0));
        scopes.remove("b"); assertEquals(Optional.of("a"),scopes.at("overworld",0,0));
        scopes.update("a","overworld",Optional.empty()); assertTrue(scopes.at("overworld",0,0).isEmpty());
    }
    @Test void touchingExclusiveEdgesDoNotOverlap() {
        var scopes=new VillageScopes<String,String>();
        scopes.update("a","world",Optional.of(VillageBounds.centered(0,0,25,25)));
        scopes.update("b","world",Optional.of(VillageBounds.centered(50,0,25,25)));
        assertTrue(scopes.active("a").isPresent()); assertTrue(scopes.active("b").isPresent());
        assertEquals(Optional.of("b"),scopes.at("world",25,0));
    }
}
