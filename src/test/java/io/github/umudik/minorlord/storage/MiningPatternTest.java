package io.github.umudik.minorlord.storage;
import io.github.umudik.minorlord.mining.*;
import io.github.umudik.minorlord.roles.*;
import net.minecraft.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MiningPatternTest {
    @Test void nativeRoleNamesNormalizeToLowercaseAndRequireWholeName(){
        assertEquals(Optional.of(Role.MINER),RoleNames.find("miner"));
        assertEquals(Optional.of(Role.CHESTER),RoleNames.find("chester"));
        assertEquals(Optional.of(Role.MINER),RoleNames.find("MINER"));
        assertEquals(Optional.of(Role.MINER),RoleNames.find("Miner"));
        assertEquals(Optional.of(Role.CHESTER),RoleNames.find("Chester"));
        assertTrue(RoleNames.find("Mineral").isEmpty());
    }
    @Test void tenBlockLeadInUsesSignMinusOneYAndSignCenteredRadius(){
        var bounds=new MiningBounds(100,64,200,1000);
        for(int i=0;i<10;i++)assertEquals(new BlockPos(100+i+1,63,200),MiningPattern.point(bounds,Direction.EAST,i).orElseThrow());
        assertEquals(new BlockPos(110,63,200),MiningPattern.point(bounds,Direction.EAST,10).orElseThrow());
    }
    @Test void spiralStaysInRadiusAndAdjacentSampleDoesNotJumpBeyondNeighbor(){
        var bounds=new MiningBounds(0,64,0,100);BlockPos previous=null;int count=0;
        for(long i=0;i<100000;i++){
            var next=MiningPattern.point(bounds,Direction.NORTH,i);if(next.isEmpty())break;
            assertTrue(bounds.containsHorizontal(next.get().getX(),next.get().getZ()));
            if(previous!=null){assertTrue(Math.abs(previous.getX()-next.get().getX())<=1);assertTrue(Math.abs(previous.getZ()-next.get().getZ())<=1);}
            previous=next.get();count++;
        }
        assertTrue(count>1000);
    }
    @Test void corridorIsTwoWideAndRingsKeepTwoSolidBlocksBetween(){
        var bounds=new MiningBounds(0,64,0,40);var forward=Direction.EAST;
        assertEquals(new BlockPos(1,63,1),MiningPattern.partner(bounds,forward,MiningPattern.point(bounds,forward,0).orElseThrow(),0));
        Set<Long> corridor=new HashSet<>();
        for(long i=0;;i++){
            var cells=MiningPattern.stepCells(bounds,forward,i);if(cells.isEmpty())break;
            for(var cell:cells){corridor.add(new BlockPos(cell.getX(),63,cell.getZ()).asLong());
                assertTrue(MiningPattern.tunnelAt(bounds,forward,cell.getX(),cell.getZ()),"step "+i+" cell "+cell);}
        }
        // Every opened column has an open horizontal neighbor: nowhere is the corridor a single block wide.
        for(long key:corridor){var c=BlockPos.of(key);int neighbors=0;
            for(var d:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))if(corridor.contains(c.relative(d).asLong()))neighbors++;
            assertTrue(neighbors>=1);
            // A 2x2 block containing c is fully open, so a villager always has two-wide room.
            boolean square=false;
            for(int ox=-1;ox<=0;ox++)for(int oz=-1;oz<=0;oz++){boolean all=true;
                for(int ax=0;ax<=1;ax++)for(int az=0;az<=1;az++)if(!corridor.contains(c.offset(ox+ax,0,oz+az).asLong()))all=false;
                if(all)square=true;}
            if(Math.hypot(c.getX(),c.getZ())<=38)assertTrue(square,"single-wide at "+c);
        }
        // Along every axis ray beyond the lead-in, open runs are separated by at least two solid blocks.
        for(var direction:List.of(new int[]{1,0},new int[]{-1,0},new int[]{0,1},new int[]{0,-1})){
            int solid=0;boolean seenOpen=false;
            for(int r=12;r<=36;r++){
                boolean open=corridor.contains(new BlockPos(direction[0]*r,63,direction[1]*r).asLong());
                if(open){if(seenOpen&&solid>0)assertTrue(solid>=2,"wall of "+solid+" at r="+r);solid=0;seenOpen=true;}else solid++;
            }
        }
        // A straight two-wide spoke runs along the mine direction from the sign through every ring.
        for(int x=1;x<=30;x++)for(int side=0;side<=1;side++)assertTrue(corridor.contains(new BlockPos(x,63,side).asLong()),"spoke gap at "+x);
        for(int k=0;k<5;k++){assertEquals(k,MiningPattern.crossingBefore(MiningPattern.crossing(k)));assertEquals(k,MiningPattern.crossingBefore(MiningPattern.crossing(k+1)-1));}
        assertEquals(-1,MiningPattern.crossingBefore(9));
        var crossingCell=MiningPattern.point(bounds,forward,MiningPattern.crossing(2)).orElseThrow();
        assertTrue(Math.abs(crossingCell.getX()-MiningPattern.crossingDistance(2))<=1&&Math.abs(crossingCell.getZ())<=1,"crossing at "+crossingCell);
        // Solid cells between rings are not part of the tunnel.
        assertFalse(MiningPattern.tunnelAt(bounds,forward,0,0));
        assertTrue(MiningPattern.mouth(bounds,forward,0,0));assertFalse(MiningPattern.mouth(bounds,forward,3,0));
    }
    @Test void normalToolSpeedRequiresRealTimeAndUnbreakableBlocksNeverBecomeInstant(){
        assertEquals(6,MiningPattern.ticks(1.5f,8,true));
        assertEquals(150,MiningPattern.ticks(1.5f,1,false));
        assertEquals(Integer.MAX_VALUE,MiningPattern.ticks(-1,8,true));
    }
}
