package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.CornerShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Undo / redo history of one block's shape. Pure Java; the block entity owns one
 * instance on the server and never persists it (history is per session).
 *
 * <p>Every change records the shape <em>before</em> it. A change made as part of a
 * <em>gesture</em> (a corner move: keys repeat while held) may be merged with the
 * previous gesture change if the two are at most {@link #BURST_TICKS} apart, so one
 * undo takes back a whole drag rather than one 1/16 step. Whole-shape actions (paste,
 * mirror, rotate, reset) are never merged: each is its own step.
 *
 * <p>Undo returns the shape to restore and moves the undone entries to the redo side;
 * recording a new change discards the redo side, like every editor.
 */
public final class ShapeHistory {

    /** Default number of recorded changes kept before the oldest are forgotten. */
    public static final int DEFAULT_CAPACITY = 64;
    /** Two gesture changes at most this many ticks apart belong to one undo step. */
    public static final long BURST_TICKS = 15;

    private record Entry(CornerShape before, long time, boolean gesture) {
    }

    /** One undone step: what to restore on redo, and the entries to put back. */
    private record UndoneStep(CornerShape target, List<Entry> entries) {
    }

    private final Deque<Entry> undo = new ArrayDeque<>();
    private final Deque<UndoneStep> redo = new ArrayDeque<>();
    private final int capacity;

    public ShapeHistory() {
        this(DEFAULT_CAPACITY);
    }

    public ShapeHistory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
    }

    /**
     * Record that the shape is about to change away from {@code before}.
     *
     * @param time    game time (ticks) of the change, for gesture merging
     * @param gesture {@code true} for a single corner-move step, {@code false} for a whole-shape action
     */
    public void record(CornerShape before, long time, boolean gesture) {
        Objects.requireNonNull(before, "before");
        redo.clear();
        undo.push(new Entry(before, time, gesture));
        while (undo.size() > capacity) {
            undo.removeLast();
        }
    }

    /**
     * Undo one step.
     *
     * @param current the block's shape right now (what redo will bring back)
     * @return the shape to restore, or empty when there is nothing to undo
     */
    public Optional<CornerShape> undo(CornerShape current) {
        Objects.requireNonNull(current, "current");
        if (undo.isEmpty()) {
            return Optional.empty();
        }
        List<Entry> popped = new ArrayList<>();
        Entry last = undo.pop();
        popped.add(last);
        while (last.gesture() && !undo.isEmpty()) {
            Entry previous = undo.peek();
            if (!previous.gesture() || last.time() - previous.time() > BURST_TICKS) {
                break;
            }
            last = undo.pop();
            popped.add(last);
        }
        redo.push(new UndoneStep(current, popped));
        return Optional.of(last.before());
    }

    /**
     * Redo the most recently undone step.
     *
     * @return the shape to restore, or empty when there is nothing to redo
     */
    public Optional<CornerShape> redo() {
        UndoneStep step = redo.poll();
        if (step == null) {
            return Optional.empty();
        }
        // Put the entries back in their original order: the last popped goes down first.
        for (int i = step.entries().size() - 1; i >= 0; i--) {
            undo.push(step.entries().get(i));
        }
        return Optional.of(step.target());
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** Number of recorded changes (not steps) on the undo side. */
    public int size() {
        return undo.size();
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}