package io.github.umudik.minorlord.storage;

import io.github.umudik.minorlord.village.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WallConstructionTest {
    record Cell(long x,int y,long z) {}
    static class Terrain implements WallConstruction.World {
        final Set<Cell> placed=new HashSet<>();
        final Set<Long> water=new HashSet<>();
        final Set<Long> blocked=new HashSet<>();
        public OptionalInt safeBase(long x,long z) { return water.contains(x)?OptionalInt.empty():OptionalInt.of((int)(70+z)); }
        public boolean canPlaceColumn(long x,int y,long z) { return !blocked.contains(x); }
        public boolean placeAir(long x,int y,long z) { return placed.add(new Cell(x,y,z)); }
    }
    @Test void perimeterHasUniqueColumnsAndApprovedEdges() {
        var p=new WallPerimeter(VillageBounds.centered(0,0,25,25));
        var columns=new HashSet<WallPerimeter.Column>();
        for(long i=0;i<p.size();i++) assertTrue(columns.add(p.at(i)));
        assertEquals(196,columns.size());
        assertTrue(columns.contains(new WallPerimeter.Column(-25,-25)));
        assertTrue(columns.contains(new WallPerimeter.Column(24,24)));
        assertFalse(columns.contains(new WallPerimeter.Column(25,24)));
    }
    @Test void followsSurfaceAndPlacesExactlyFourHighWithBoundedWork() {
        var wall=new WallConstruction(VillageBounds.centered(0,0,2,2)); var world=new Terrain();
        assertEquals(8,wall.tick(world,2,()->true)); assertEquals(2,wall.examined());
        while(!wall.finished()) wall.tick(world,2,()->true);
        assertEquals(48,world.placed.size());
        for(var c:world.placed) assertTrue(c.y>=70+c.z && c.y<74+c.z);
    }
    @Test void waterAndObstructionColumnsAreLeftUntouched() {
        var wall=new WallConstruction(VillageBounds.centered(0,0,2,2)); var world=new Terrain();
        world.water.add(-2L); world.blocked.add(1L);
        while(!wall.finished()) wall.tick(world,3,()->true);
        assertEquals(16,world.placed.size());
        assertTrue(world.placed.stream().noneMatch(c->c.x==-2 || c.x==1));
    }
    @Test void invalidScopeStopsBeforeAnyWorldMutation() {
        var wall=new WallConstruction(VillageBounds.centered(0,0,2,2)); var world=new Terrain();
        assertEquals(0,wall.tick(world,8,()->false)); assertEquals(0,wall.examined());
        assertTrue(world.placed.isEmpty());
    }
    @Test void hugeBoundsStayLazyWithoutIntegerWrap() {
        var p=new WallPerimeter(VillageBounds.centered(Integer.MAX_VALUE,0,Integer.MAX_VALUE,1));
        assertEquals(8589934588L,p.size());
        assertEquals(new WallPerimeter.Column(0,0),p.at(p.size()-1));
    }
    @Test void loadedChunkSlicesCoverEveryBoundaryOnceWithoutScanningTheWholeVillage(){
        var world=new Terrain();var bounds=VillageBounds.centered(0,0,25,25);
        for(int x=-2;x<=1;x++)for(int z=-2;z<=1;z++){
            var slice=WallConstruction.inChunk(bounds,x*16,x*16+15,z*16,z*16+15);
            while(!slice.finished())slice.tick(world,8,()->true);
            assertTrue(slice.examined()<=64);
        }
        assertEquals(196*4,world.placed.size());
    }
}
