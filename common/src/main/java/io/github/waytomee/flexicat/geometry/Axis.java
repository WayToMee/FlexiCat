package io.github.waytomee.flexicat.geometry;

/**
 * Coordinate axis, independent of Minecraft classes so the geometry core can be
 * unit-tested on a plain JVM. Ordinal order (X, Y, Z) matches the corner index bits.
 */
public enum Axis {
    X, Y, Z;

    private static final Axis[] VALUES = values();

    public static Axis byOrdinal(int ordinal) {
        return VALUES[ordinal];
    }
}