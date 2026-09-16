package io.github.waytomee.flexicat.geometry;

/**
 * One of the six faces of a shape cell, with its four corners listed in a fixed
 * counter-clockwise order as seen from outside the (undeformed) cube.
 *
 * <p>The order is part of the shape's contract: rendering, ray casting and the
 * triangulation diagonal all rely on it, so it must not change between versions.
 */
public enum CubeFace {
    DOWN(Axis.Y, false,
            Corner.DOWN_WEST_NORTH, Corner.DOWN_EAST_NORTH, Corner.DOWN_EAST_SOUTH, Corner.DOWN_WEST_SOUTH),
    UP(Axis.Y, true,
            Corner.UP_WEST_SOUTH, Corner.UP_EAST_SOUTH, Corner.UP_EAST_NORTH, Corner.UP_WEST_NORTH),
    NORTH(Axis.Z, false,
            Corner.UP_EAST_NORTH, Corner.DOWN_EAST_NORTH, Corner.DOWN_WEST_NORTH, Corner.UP_WEST_NORTH),
    SOUTH(Axis.Z, true,
            Corner.UP_WEST_SOUTH, Corner.DOWN_WEST_SOUTH, Corner.DOWN_EAST_SOUTH, Corner.UP_EAST_SOUTH),
    WEST(Axis.X, false,
            Corner.UP_WEST_NORTH, Corner.DOWN_WEST_NORTH, Corner.DOWN_WEST_SOUTH, Corner.UP_WEST_SOUTH),
    EAST(Axis.X, true,
            Corner.UP_EAST_SOUTH, Corner.DOWN_EAST_SOUTH, Corner.DOWN_EAST_NORTH, Corner.UP_EAST_NORTH);

    private static final CubeFace[] VALUES = values();

    private final Axis axis;
    private final boolean max;
    private final Corner[] corners;

    CubeFace(Axis axis, boolean max, Corner a, Corner b, Corner c, Corner d) {
        this.axis = axis;
        this.max = max;
        this.corners = new Corner[] {a, b, c, d};
    }

    /** Axis this face is perpendicular to in the undeformed cube. */
    public Axis axis() {
        return axis;
    }

    /** {@code true} for UP / SOUTH / EAST. */
    public boolean isMax() {
        return max;
    }

    /** The four corners of this face, counter-clockwise from outside. */
    public Corner corner(int i) {
        return corners[i];
    }

    public Corner[] corners() {
        return corners.clone();
    }

    public boolean contains(Corner corner) {
        return corner.isMax(axis) == max;
    }

    public CubeFace opposite() {
        return VALUES[ordinal() ^ 1];
    }

    public static CubeFace of(Axis axis, boolean max) {
        // Declaration order is Y (DOWN/UP), Z (NORTH/SOUTH), X (WEST/EAST) to match
        // Minecraft's Direction order, so this is an explicit lookup, not arithmetic.
        return switch (axis) {
            case Y -> max ? UP : DOWN;
            case Z -> max ? SOUTH : NORTH;
            case X -> max ? EAST : WEST;
        };
    }
}