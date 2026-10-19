package io.github.umudik.minorlord.village;

/** Approved half-open horizontal block bounds: [center-X, center+X), [center-Z, center+Z). */
public record VillageBounds(long minX, long maxXExclusive, long minZ, long maxZExclusive) {
    public VillageBounds {
        if (minX >= maxXExclusive || minZ >= maxZExclusive) throw new IllegalArgumentException("Empty village");
    }
    public static VillageBounds centered(int x, int z, int halfX, int halfZ) {
        if (halfX <= 0 || halfZ <= 0) throw new IllegalArgumentException("Positive village extents required");
        return new VillageBounds((long) x-halfX, (long) x+halfX, (long) z-halfZ, (long) z+halfZ);
    }
    public boolean contains(int x, int z) {
        return x >= minX && x < maxXExclusive && z >= minZ && z < maxZExclusive;
    }
    public boolean overlaps(VillageBounds other) {
        return minX < other.maxXExclusive && other.minX < maxXExclusive
                && minZ < other.maxZExclusive && other.minZ < maxZExclusive;
    }
}
