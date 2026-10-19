package io.github.umudik.minorlord.village;

import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

/** Incremental, air-only four-block wall. Terrain/water/protection are checked by the world adapter. */
public final class WallConstruction {
    public interface World {
        /** Bottom wall Y above safe dry natural ground; empty for water/unloaded/protected/uncertain terrain. */
        OptionalInt safeBase(long x,long z);
        /** Whole column clearance, protection and entity occupancy must be rechecked before mutation. */
        boolean canPlaceColumn(long x,int y,long z);
        /** Rechecks current air/protection before placing one block; never replaces any existing block. */
        boolean placeAir(long x,int y,long z);
    }
    private final WallPerimeter perimeter;
    private java.util.List<WallPerimeter.Column> clipped;
    private long next;
    public WallConstruction(VillageBounds bounds) { perimeter=new WallPerimeter(bounds); }
    public static WallConstruction inChunk(VillageBounds bounds,int minX,int maxX,int minZ,int maxZ){
        var result=new WallConstruction(bounds);var columns=new java.util.LinkedHashSet<WallPerimeter.Column>();
        for(long x=Math.max(bounds.minX(),minX);x<=Math.min(bounds.maxXExclusive()-1,maxX);x++){
            if(bounds.minZ()>=minZ&&bounds.minZ()<=maxZ)columns.add(new WallPerimeter.Column(x,bounds.minZ()));
            long z=bounds.maxZExclusive()-1;if(z>=minZ&&z<=maxZ)columns.add(new WallPerimeter.Column(x,z));
        }
        for(long z=Math.max(bounds.minZ(),minZ);z<=Math.min(bounds.maxZExclusive()-1,maxZ);z++){
            if(bounds.minX()>=minX&&bounds.minX()<=maxX)columns.add(new WallPerimeter.Column(bounds.minX(),z));
            long x=bounds.maxXExclusive()-1;if(x>=minX&&x<=maxX)columns.add(new WallPerimeter.Column(x,z));
        }
        result.clipped=java.util.List.copyOf(columns);return result;
    }
    public boolean finished() { return next==(clipped==null?perimeter.size():clipped.size()); }
    public long examined() { return next; }
    public int tick(World world, int columnBudget, BooleanSupplier stillActive) {
        if(columnBudget<=0) throw new IllegalArgumentException("Positive processing budget required");
        int placed=0;
        for(int i=0;i<columnBudget && !finished() && stillActive.getAsBoolean();i++) {
            var column=clipped==null?perimeter.at(next++):clipped.get((int)next++);
            var base=world.safeBase(column.x(),column.z());
            if(base.isEmpty() || !world.canPlaceColumn(column.x(),base.getAsInt(),column.z())) continue;
            for(int h=0;h<4 && stillActive.getAsBoolean();h++) {
                if(!world.placeAir(column.x(),Math.addExact(base.getAsInt(),h),column.z())) break;
                placed++;
            }
        }
        return placed;
    }
}
