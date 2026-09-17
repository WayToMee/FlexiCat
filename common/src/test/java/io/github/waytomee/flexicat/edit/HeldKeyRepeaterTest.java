package io.github.waytomee.flexicat.edit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeldKeyRepeaterTest {

    /** Ticks (0-based) on which slot 0 fires while held for {@code ticks} ticks. */
    private static List<Integer> firing(HeldKeyRepeater r, int ticks) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < ticks; i++) {
            if (r.tick(0, true)) {
                out.add(i);
            }
        }
        return out;
    }

    @Test
    void firesOncePerTapAndNeverWhileReleased() {
        HeldKeyRepeater r = new HeldKeyRepeater(1, 6, 2);
        assertFalse(r.tick(0, false));
        assertTrue(r.tick(0, true));
        assertFalse(r.tick(0, false));
        assertTrue(r.tick(0, true), "a release and a new press fire again immediately");
    }

    @Test
    void repeatsAfterInitialDelayAtFixedInterval() {
        HeldKeyRepeater r = new HeldKeyRepeater(1, 6, 2);
        assertEquals(List.of(0, 6, 8, 10, 12), firing(r, 14));
    }

    @Test
    void shortTapDoesNotRepeat() {
        HeldKeyRepeater r = new HeldKeyRepeater(1, 6, 2);
        assertEquals(List.of(0), firing(r, 5));
        assertFalse(r.tick(0, false));
    }

    @Test
    void slotsAreIndependent() {
        HeldKeyRepeater r = new HeldKeyRepeater(2, 3, 1);
        assertTrue(r.tick(0, true));
        assertFalse(r.tick(1, false));
        assertFalse(r.tick(0, true));
        assertTrue(r.tick(1, true), "slot 1 pressed later fires on its own press tick");
        assertFalse(r.tick(0, true));
        assertTrue(r.tick(0, true), "slot 0 reaches its delay regardless of slot 1");
    }

    @Test
    void resetForgetsHeldKeys() {
        HeldKeyRepeater r = new HeldKeyRepeater(1, 2, 1);
        assertTrue(r.tick(0, true));
        r.reset();
        assertTrue(r.tick(0, true), "after reset a held key counts as a fresh press");
    }

    @Test
    void rejectsNonPositiveParameters() {
        assertThrows(IllegalArgumentException.class, () -> new HeldKeyRepeater(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeldKeyRepeater(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeldKeyRepeater(1, 1, 0));
    }
}