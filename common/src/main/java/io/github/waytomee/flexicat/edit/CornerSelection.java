package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Corner;

import java.util.List;

/**
 * The set of corners that receive move keys while editing: zero, one or several.
 *
 * <p>Plain right-click on a handle {@link #select selects} it alone (replacing the set);
 * a modifier-click {@link #toggle toggles} it in or out, which is how a group is built
 * — for example the four top corners to lower a whole face at once. Stored as a corner
 * mask so it can go straight into a move packet. Pure Java, unit-tested.
 */
public final class CornerSelection {

    private int mask;

    public int mask() {
        return mask;
    }

    public boolean isEmpty() {
        return mask == 0;
    }

    public int count() {
        return Integer.bitCount(mask);
    }

    public boolean contains(Corner corner) {
        return (mask & corner.bit()) != 0;
    }

    /** Corners in the set, in index order. */
    public List<Corner> corners() {
        return Corner.fromMask(mask);
    }

    /** The only member, or {@code null} unless exactly one corner is selected. */
    public Corner single() {
        return count() == 1 ? Corner.byIndex(Integer.numberOfTrailingZeros(mask)) : null;
    }

    /** Make {@code corner} the whole selection. */
    public void select(Corner corner) {
        mask = corner.bit();
    }

    /** Add {@code corner} if absent, remove it if present. */
    public void toggle(Corner corner) {
        mask ^= corner.bit();
    }

    public void clear() {
        mask = 0;
    }
}