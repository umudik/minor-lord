package io.github.umudik.minorlord.mining;

/** Mine-sign-centered radius and sign-Y-minus-one math; no chunk loading or voxel pattern choice. */
public record MiningBounds(int signX, int signY, int signZ, int radius) {
    public MiningBounds {
        if (radius <= 0) throw new IllegalArgumentException("Positive radius required");
    }

    public int bottomY() { return Math.subtractExact(signY, 1); }

    public boolean containsHorizontal(int x, int z) {
        long dx = (long) x - signX;
        long dz = (long) z - signZ;
        if (Math.abs(dx) > radius || Math.abs(dz) > radius) return false;
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
