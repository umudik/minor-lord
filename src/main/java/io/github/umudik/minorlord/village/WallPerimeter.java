package io.github.umudik.minorlord.village;

/** Lazy perimeter: each boundary column exactly once, without allocating an area-sized list. */
public final class WallPerimeter {
    public record Column(long x, long z) {}
    private final VillageBounds bounds;
    private final long width, depth;
    public WallPerimeter(VillageBounds bounds) {
        this.bounds=bounds;
        width=bounds.maxXExclusive()-bounds.minX(); depth=bounds.maxZExclusive()-bounds.minZ();
    }
    public long size() {
        if(width==1) return depth;
        if(depth==1) return width;
        return Math.addExact(Math.multiplyExact(2,width),Math.multiplyExact(2,depth))-4;
    }
    public Column at(long index) {
        if(index<0 || index>=size()) throw new IndexOutOfBoundsException();
        if(width==1) return new Column(bounds.minX(),bounds.minZ()+index);
        if(depth==1) return new Column(bounds.minX()+index,bounds.minZ());
        if(index<width) return new Column(bounds.minX()+index,bounds.minZ());
        index-=width;
        if(index<depth-1) return new Column(bounds.maxXExclusive()-1,bounds.minZ()+1+index);
        index-=depth-1;
        if(index<width-1) return new Column(bounds.maxXExclusive()-2-index,bounds.maxZExclusive()-1);
        index-=width-1;
        return new Column(bounds.minX(),bounds.maxZExclusive()-2-index);
    }
}
