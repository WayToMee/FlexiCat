package io.github.waytomee.flexicat.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the eight corners of a shape cell.
 *
 * <p>Corners are identified by a stable index {@code 0..7} where bit 0 selects
 * the X side, bit 1 the Y side and bit 2 the Z side ({@code 0} = min face,
 * {@code 1} = max face). The index never changes when a corner is moved, so two
 * corners that end up at the same position are still two distinct corners.
 *
 * <pre>
 *   index  x y z   name
 *     0    - - -   DOWN_WEST_NORTH
 *     1    + - -   DOWN_EAST_NORTH
 *     2    - + -   UP_WEST_NORTH
 *     3    + + -   UP_EAST_NORTH
 *     4    - - +   DOWN_WEST_SOUTH
 *     5    + - +   DOWN_EAST_SOUTH
 *     6    - + +   UP_WEST_SOUTH
 *     7    + + +   UP_EAST_SOUTH
 * </pre>
 *
 * <p>A <em>set</em> of corners is passed around as an 8-bit mask (bit {@code i} =
 * corner {@code i}); see {@link #mask(Iterable)} and {@link #fromMask(int)}.
 */
public enum Corner {
    DOWN_WEST_NORTH(0),
    DOWN_EAST_NORTH(1),
    UP_WEST_NORTH(2),
    UP_EAST_NORTH(3),
    DOWN_WEST_SOUTH(4),
    DOWN_EAST_SOUTH(5),
    UP_WEST_SOUTH(6),
    UP_EAST_SOUTH(7);

    public static final int COUNT = 8;
    /** Mask with every corner set. */
    public static final int ALL_MASK = (1 << COUNT) - 1;
    private static final Corner[] BY_INDEX = values();

    private final int index;

    Corner(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    /** This corner's bit in a corner mask. */
    public int bit() {
        return 1 << index;
    }

    /** {@code true} if this corner sits on the max side of the given axis. */
    public boolean isMax(Axis axis) {
        return ((index >> axis.ordinal()) & 1) == 1;
    }

    /** Rest position of this corner on the given axis, in {@link CornerShape#GRID} units. */
    public int restPosition(Axis axis) {
        return isMax(axis) ? CornerShape.GRID : 0;
    }

    public static Corner byIndex(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("Corner index out of range: " + index);
        }
        return BY_INDEX[index];
    }

    /** Corner index built from per-axis sides ({@code true} = max side). */
    public static Corner of(boolean maxX, boolean maxY, boolean maxZ) {
        return BY_INDEX[(maxX ? 1 : 0) | (maxY ? 2 : 0) | (maxZ ? 4 : 0)];
    }

    /** The corner on the opposite side of the given axis (other coordinates unchanged). */
    public Corner across(Axis axis) {
        return BY_INDEX[index ^ (1 << axis.ordinal())];
    }

    /** Bit mask of the given corners. */
    public static int mask(Iterable<Corner> corners) {
        int m = 0;
        for (Corner c : corners) {
            m |= c.bit();
        }
        return m;
    }

    /** {@code true} if {@code mask} uses only the eight corner bits and is not empty. */
    public static boolean isValidMask(int mask) {
        return mask > 0 && mask <= ALL_MASK;
    }

    /** The corners whose bits are set, in index order. Bits above the eighth are ignored. */
    public static List<Corner> fromMask(int mask) {
        List<Corner> out = new ArrayList<>(Integer.bitCount(mask & ALL_MASK));
        for (Corner c : BY_INDEX) {
            if ((mask & c.bit()) != 0) {
                out.add(c);
            }
        }
        return out;
    }
}