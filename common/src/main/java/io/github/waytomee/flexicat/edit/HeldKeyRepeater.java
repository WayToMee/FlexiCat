package io.github.waytomee.flexicat.edit;

import java.util.Arrays;

/**
 * Turns "is this key held right now?" samples, taken once per tick, into a stream of
 * repeat events: one on the tick the key goes down, then — after an initial delay —
 * one every {@code repeatInterval} ticks for as long as it stays down.
 *
 * <p>Why not the OS key repeat: GLFW only repeats the <em>last</em> key pressed, so
 * holding an arrow key while also holding a movement key (walking around the block)
 * produces no repeat events at all. Sampling the down state instead makes moves
 * independent of what else is held, and the fixed interval also caps how often a
 * move (and with it a shape rebuild and chunk re-mesh) can be requested.
 *
 * <p>Pure Java, no Minecraft classes, so it can be unit-tested directly.
 */
public final class HeldKeyRepeater {

    /** Ticks a key must stay held before it starts repeating (~300 ms at 20 TPS). */
    public static final int DEFAULT_INITIAL_DELAY = 6;
    /** Ticks between repeats once repeating (~10 moves per second). */
    public static final int DEFAULT_REPEAT_INTERVAL = 2;

    private static final int RELEASED = -1;

    private final int initialDelay;
    private final int repeatInterval;
    /** Ticks each slot has been held, or {@link #RELEASED}. */
    private final int[] held;

    public HeldKeyRepeater(int slots, int initialDelay, int repeatInterval) {
        if (slots < 1 || initialDelay < 1 || repeatInterval < 1) {
            throw new IllegalArgumentException("slots, initialDelay and repeatInterval must be positive");
        }
        this.initialDelay = initialDelay;
        this.repeatInterval = repeatInterval;
        this.held = new int[slots];
        reset();
    }

    public int slots() {
        return held.length;
    }

    /**
     * Feed one tick's sample for {@code slot}.
     *
     * @return {@code true} if the key's action should fire this tick
     */
    public boolean tick(int slot, boolean down) {
        if (!down) {
            held[slot] = RELEASED;
            return false;
        }
        int t = ++held[slot];
        if (t == 0) {
            return true; // just pressed
        }
        return t >= initialDelay && (t - initialDelay) % repeatInterval == 0;
    }

    /** Forget every held key (editing stopped, a screen opened, ...). */
    public void reset() {
        Arrays.fill(held, RELEASED);
    }
}