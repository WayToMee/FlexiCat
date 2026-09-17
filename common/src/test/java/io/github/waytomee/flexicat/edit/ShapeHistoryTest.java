package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ShapeHistoryTest {

    private static final CornerShape CUBE = CornerShape.cube();
    private static final CornerShape ONE = CUBE.move(Corner.UP_WEST_NORTH, Axis.Y, -1);
    private static final CornerShape TWO = ONE.move(Corner.UP_WEST_NORTH, Axis.Y, -1);
    private static final CornerShape THREE = TWO.move(Corner.UP_WEST_NORTH, Axis.Y, -1);
    private static final CornerShape MIRRORED = THREE.mirror(Axis.X);

    @Test
    void startsEmpty() {
        ShapeHistory h = new ShapeHistory();
        assertFalse(h.canUndo());
        assertFalse(h.canRedo());
        assertEquals(Optional.empty(), h.undo(CUBE));
        assertEquals(Optional.empty(), h.redo());
    }

    @Test
    void wholeShapeActionsAreSeparateSteps() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, false);      // cube → ONE (paste)
        h.record(ONE, 101, false);       // ONE → MIRRORED (mirror), right after
        assertEquals(Optional.of(ONE), h.undo(MIRRORED));
        assertEquals(Optional.of(CUBE), h.undo(ONE));
        assertFalse(h.canUndo());
    }

    @Test
    void heldKeyBurstIsOneStep() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, true);       // cube → ONE
        h.record(ONE, 102, true);        // ONE → TWO
        h.record(TWO, 104, true);        // TWO → THREE
        assertEquals(Optional.of(CUBE), h.undo(THREE), "one undo takes back the whole drag");
        assertFalse(h.canUndo());
        assertEquals(Optional.of(THREE), h.redo(), "and one redo brings it all back");
        assertEquals(3, h.size());
    }

    @Test
    void pauseSplitsGestures() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, true);
        h.record(ONE, 102, true);
        h.record(TWO, 102 + ShapeHistory.BURST_TICKS + 1, true);
        assertEquals(Optional.of(TWO), h.undo(THREE), "the late step is its own");
        assertEquals(Optional.of(CUBE), h.undo(TWO), "then the earlier burst");
    }

    @Test
    void gestureDoesNotMergeAcrossWholeShapeAction() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, true);       // move
        h.record(ONE, 101, false);       // paste
        h.record(TWO, 102, true);        // move
        assertEquals(Optional.of(TWO), h.undo(THREE));
        assertEquals(Optional.of(ONE), h.undo(TWO));
        assertEquals(Optional.of(CUBE), h.undo(ONE));
    }

    @Test
    void redoIsUndoneByUndoAgain() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, false);
        h.record(ONE, 200, false);
        assertEquals(Optional.of(ONE), h.undo(TWO));
        assertEquals(Optional.of(TWO), h.redo());
        assertFalse(h.canRedo());
        assertEquals(Optional.of(ONE), h.undo(TWO));
        assertEquals(Optional.of(CUBE), h.undo(ONE));
        assertEquals(Optional.of(ONE), h.redo());
        assertEquals(Optional.of(TWO), h.redo());
        assertFalse(h.canRedo());
    }

    @Test
    void newChangeDiscardsRedo() {
        ShapeHistory h = new ShapeHistory();
        h.record(CUBE, 100, false);
        assertEquals(Optional.of(CUBE), h.undo(ONE));
        assertTrue(h.canRedo());
        h.record(CUBE, 300, false);      // cube → MIRRORED instead
        assertFalse(h.canRedo());
        assertEquals(Optional.of(CUBE), h.undo(MIRRORED));
    }

    @Test
    void capacityForgetsTheOldest() {
        ShapeHistory h = new ShapeHistory(2);
        h.record(CUBE, 100, false);
        h.record(ONE, 200, false);
        h.record(TWO, 300, false);
        assertEquals(2, h.size());
        assertEquals(Optional.of(TWO), h.undo(THREE));
        assertEquals(Optional.of(ONE), h.undo(TWO));
        assertFalse(h.canUndo(), "the cube state fell off the end");
        assertThrows(IllegalArgumentException.class, () -> new ShapeHistory(0));
    }
}