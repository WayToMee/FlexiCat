package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Axis;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CornerMoveTest {

    @Test
    void mapsWorldDirectionsToAxisSteps() {
        assertEquals(new CornerMove(Axis.X, 1), CornerMove.fromDirection(Direction.EAST));
        assertEquals(new CornerMove(Axis.X, -1), CornerMove.fromDirection(Direction.WEST));
        assertEquals(new CornerMove(Axis.Y, 1), CornerMove.fromDirection(Direction.UP));
        assertEquals(new CornerMove(Axis.Y, -1), CornerMove.fromDirection(Direction.DOWN));
        assertEquals(new CornerMove(Axis.Z, 1), CornerMove.fromDirection(Direction.SOUTH));
        assertEquals(new CornerMove(Axis.Z, -1), CornerMove.fromDirection(Direction.NORTH));
    }

    @Test
    void viewRelativeKeysResolveAgainstFacing() {
        // Facing north: "right" is east, "left" is west, "away" is north.
        Direction facing = Direction.NORTH;
        assertEquals(new CornerMove(Axis.X, 1), CornerMove.fromDirection(facing.getClockWise()));
        assertEquals(new CornerMove(Axis.X, -1), CornerMove.fromDirection(facing.getCounterClockWise()));
        assertEquals(new CornerMove(Axis.Z, -1), CornerMove.fromDirection(facing));
        assertEquals(new CornerMove(Axis.Z, 1), CornerMove.fromDirection(facing.getOpposite()));
    }
}