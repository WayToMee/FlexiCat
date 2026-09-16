package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HandlePickerTest {

    private static final double H = HandlePicker.DEFAULT_HALF_SIZE;

    private static Optional<HandlePicker.Hit> pick(CornerShape shape,
                                                   double ox, double oy, double oz,
                                                   double dx, double dy, double dz) {
        return HandlePicker.pick(shape, ox, oy, oz, dx, dy, dz, H, 10.0);
    }

    @Test
    void hitsTheCornerAimedAt() {
        // From outside the cube, looking straight at the (1,1,1) corner.
        Optional<HandlePicker.Hit> hit = pick(CornerShape.cube(), 3, 3, 3, -1, -1, -1);
        assertEquals(Corner.UP_EAST_SOUTH, hit.orElseThrow().corner());
    }

    @Test
    void missesBetweenHandles() {
        // Straight down through the middle of the top face: every handle is 0.5 away.
        assertTrue(pick(CornerShape.cube(), 0.5, 3, 0.5, 0, -1, 0).isEmpty());
    }

    @Test
    void nearestHandleWins() {
        // Along the bottom north edge from the west: hits the west corner before the east one.
        Optional<HandlePicker.Hit> hit = pick(CornerShape.cube(), -2, 0, 0, 1, 0, 0);
        assertEquals(Corner.DOWN_WEST_NORTH, hit.orElseThrow().corner());
        assertEquals(2 - H, hit.get().distance(), 1e-9);
    }

    @Test
    void followsMovedCorner() {
        // Lower the top north-east corner half way: it now sits at (16, 8, 0).
        CornerShape shape = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8);
        // A ray at y = 0.5 along the north edge hits the moved handle, not the untouched ones.
        Optional<HandlePicker.Hit> hit = pick(shape, 3, 0.5, 0, -1, 0, 0);
        assertEquals(Corner.UP_EAST_NORTH, hit.orElseThrow().corner());
        // The same ray on the unmoved cube passes between the bottom and top corners.
        assertTrue(pick(CornerShape.cube(), 3, 0.5, 0, -1, 0, 0).isEmpty());
    }

    @Test
    void ignoresHandlesBeyondMaxDistance() {
        assertTrue(HandlePicker.pick(CornerShape.cube(), 3, 3, 3, -1, -1, -1, H, 1.0).isEmpty());
    }

    @Test
    void ignoresHandlesBehindTheRay() {
        assertTrue(pick(CornerShape.cube(), 3, 3, 3, 1, 1, 1).isEmpty());
    }

    @Test
    void originInsideHandleHitsAtZero() {
        Optional<HandlePicker.Hit> hit = pick(CornerShape.cube(), 0, 0, 0, 0, 1, 0);
        assertEquals(Corner.DOWN_WEST_NORTH, hit.orElseThrow().corner());
        assertEquals(0.0, hit.get().distance(), 1e-12);
    }

    @Test
    void zeroDirectionPicksNothing() {
        assertTrue(pick(CornerShape.cube(), 3, 3, 3, 0, 0, 0).isEmpty());
    }
}