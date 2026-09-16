package io.github.waytomee.flexicat.geometry;

/**
 * Integer position on the {@link CornerShape#GRID} lattice (1/16 of a block).
 */
public record Vec3i16(int x, int y, int z) {

    public int get(Axis axis) {
        return switch (axis) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    public Vec3i16 with(Axis axis, int value) {
        return switch (axis) {
            case X -> new Vec3i16(value, y, z);
            case Y -> new Vec3i16(x, value, z);
            case Z -> new Vec3i16(x, y, value);
        };
    }

    public Vec3i16 add(Vec3i16 other) {
        return new Vec3i16(x + other.x, y + other.y, z + other.z);
    }

    public Vec3i16 sub(Vec3i16 other) {
        return new Vec3i16(x - other.x, y - other.y, z - other.z);
    }

    /** Cross product (integer, may be large). */
    public Vec3i16 cross(Vec3i16 o) {
        return new Vec3i16(
                y * o.z - z * o.y,
                z * o.x - x * o.z,
                x * o.y - y * o.x);
    }

    public long dot(Vec3i16 o) {
        return (long) x * o.x + (long) y * o.y + (long) z * o.z;
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    /** Position in block units (1.0 = one full block). */
    public double xBlocks() {
        return x / (double) CornerShape.GRID;
    }

    public double yBlocks() {
        return y / (double) CornerShape.GRID;
    }

    public double zBlocks() {
        return z / (double) CornerShape.GRID;
    }
}