package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Corner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CornerSelectionTest {

    @Test
    void startsEmpty() {
        CornerSelection s = new CornerSelection();
        assertTrue(s.isEmpty());
        assertEquals(0, s.count());
        assertNull(s.single());
        assertEquals(List.of(), s.corners());
    }

    @Test
    void selectReplacesTheWholeSet() {
        CornerSelection s = new CornerSelection();
        s.toggle(Corner.UP_WEST_NORTH);
        s.toggle(Corner.UP_EAST_NORTH);
        s.select(Corner.DOWN_EAST_SOUTH);
        assertEquals(1, s.count());
        assertEquals(Corner.DOWN_EAST_SOUTH, s.single());
        assertEquals(Corner.DOWN_EAST_SOUTH.bit(), s.mask());
    }

    @Test
    void toggleBuildsAGroupAndRemovesAgain() {
        CornerSelection s = new CornerSelection();
        s.toggle(Corner.UP_WEST_NORTH);
        s.toggle(Corner.UP_EAST_SOUTH);
        assertEquals(2, s.count());
        assertNull(s.single(), "single() only for exactly one corner");
        assertTrue(s.contains(Corner.UP_WEST_NORTH));
        assertTrue(s.contains(Corner.UP_EAST_SOUTH));
        assertEquals(List.of(Corner.UP_WEST_NORTH, Corner.UP_EAST_SOUTH), s.corners());
        s.toggle(Corner.UP_WEST_NORTH);
        assertEquals(Corner.UP_EAST_SOUTH, s.single());
        s.clear();
        assertTrue(s.isEmpty());
    }

    @Test
    void masksRoundTripThroughCorner() {
        List<Corner> top = List.of(Corner.UP_WEST_NORTH, Corner.UP_EAST_NORTH, Corner.UP_WEST_SOUTH, Corner.UP_EAST_SOUTH);
        int mask = Corner.mask(top);
        assertEquals(0b11001100, mask);
        assertEquals(top, Corner.fromMask(mask));
        assertTrue(Corner.isValidMask(mask));
        assertFalse(Corner.isValidMask(0));
        assertFalse(Corner.isValidMask(256));
        assertEquals(List.of(), Corner.fromMask(0));
    }
}