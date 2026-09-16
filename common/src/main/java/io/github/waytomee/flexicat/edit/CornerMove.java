package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Axis;
import net.minecraft.core.Direction;

/**
 * One grid step of a corner along a world axis. The client resolves view-relative
 * keys (left/right/away/towards) to world directions and then to this.
 */
public record CornerMove(Axis axis, int delta) {

    public static final CornerMove UP = new CornerMove(Axis.Y, 1);
    public static final CornerMove DOWN = new CornerMove(Axis.Y, -1);

    /** One step in the given world direction. */
    public static CornerMove fromDirection(Direction direction) {
        Axis axis = switch (direction.getAxis()) {
            case X -> Axis.X;
            case Y -> Axis.Y;
            case Z -> Axis.Z;
        };
        return new CornerMove(axis, direction.getAxisDirection().getStep());
    }
}