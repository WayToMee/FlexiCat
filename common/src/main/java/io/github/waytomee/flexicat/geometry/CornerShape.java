package io.github.waytomee.flexicat.geometry;

import java.util.Arrays;

/**
 * The shape of one FlexiCat block: eight corner positions on a 1/16 grid.
 *
 * <p>This is the whole model of the first version. There are no presets — a shape
 * is whatever the eight corners say it is. The class is immutable; every edit
 * returns a new instance, which keeps undo, networking and copying trivial.
 *
 * <h2>Coordinate contract</h2>
 * <ul>
 *   <li>Positions are integers in {@link #GRID} units ({@code 16} = one block).</li>
 *   <li>In version 1 every corner stays inside its own block cell,
 *       {@code [MIN, MAX]} on each axis. Whether corners may later leave the cell
 *       is an open design question; the bounds live in one place so that decision
 *       can be changed without touching callers.</li>
 *   <li>A corner keeps its {@link Corner} identity no matter where it is moved,
 *       so coincident corners remain distinct and can be separated again.</li>
 * </ul>
 *
 * <h2>Serialised form</h2>
 * {@link #toBytes()} / {@link #fromBytes(byte[])} — 24 bytes, corner-major
 * (index 0..7), axis-minor (x, y, z). Loader-specific codecs wrap this.
 */
public final class CornerShape {

    /** Grid resolution: positions per block edge. */
    public static final int GRID = 16;
    /** Lowest allowed coordinate (version 1: block cell boundary). */
    public static final int MIN = 0;
    /** Highest allowed coordinate (version 1: block cell boundary). */
    public static final int MAX = GRID;
    /** Length of {@link #toBytes()}. */
    public static final int BYTES = Corner.COUNT * 3;

    private static final CornerShape CUBE = new CornerShape(restPositions());

    /** Corner-major, axis-minor. Never exposed mutable. */
    private final int[] data;

    private CornerShape(int[] data) {
        this.data = data;
    }

    /** The undeformed full block. */
    public static CornerShape cube() {
        return CUBE;
    }

    private static int[] restPositions() {
        int[] d = new int[BYTES];
        for (Corner c : Corner.values()) {
            for (Axis a : Axis.values()) {
                d[c.index() * 3 + a.ordinal()] = c.restPosition(a);
            }
        }
        return d;
    }

    // --- queries ---------------------------------------------------------------

    public Vec3i16 position(Corner corner) {
        int base = corner.index() * 3;
        return new Vec3i16(data[base], data[base + 1], data[base + 2]);
    }

    public int position(Corner corner, Axis axis) {
        return data[corner.index() * 3 + axis.ordinal()];
    }

    public boolean isCube() {
        return this.equals(CUBE);
    }

    /** Offset of a corner from its rest position (all zero for a cube). */
    public Vec3i16 offset(Corner corner) {
        Vec3i16 p = position(corner);
        return new Vec3i16(p.x() - corner.restPosition(Axis.X),
                p.y() - corner.restPosition(Axis.Y),
                p.z() - corner.restPosition(Axis.Z));
    }

    /** Axis-aligned bounds of all eight corners, as {min, max} per axis in grid units. */
    public int[] bounds() {
        int[] b = {MAX, MIN, MAX, MIN, MAX, MIN};
        for (Corner c : Corner.values()) {
            for (Axis a : Axis.values()) {
                int v = position(c, a);
                int i = a.ordinal() * 2;
                if (v < b[i]) b[i] = v;
                if (v > b[i + 1]) b[i + 1] = v;
            }
        }
        return b;
    }

    /** How many corners are away from their rest position (0 for a cube, up to 8). */
    public int movedCorners() {
        int n = 0;
        for (Corner c : Corner.values()) {
            if (!offset(c).isZero()) {
                n++;
            }
        }
        return n;
    }

    /** {@code true} if every corner sits on the same coordinate along {@code axis} — the shape has no volume. */
    public boolean isFlat() {
        int[] b = bounds();
        for (Axis a : Axis.values()) {
            if (b[a.ordinal() * 2] == b[a.ordinal() * 2 + 1]) {
                return true;
            }
        }
        return false;
    }

    /** Geometry of one face given the current corner positions. */
    public FaceQuad face(CubeFace face) {
        return new FaceQuad(face,
                position(face.corner(0)), position(face.corner(1)),
                position(face.corner(2)), position(face.corner(3)));
    }

    // --- edits -----------------------------------------------------------------

    /** Move one corner by {@code delta} grid steps along {@code axis}, clamped to the cell. */
    public CornerShape move(Corner corner, Axis axis, int delta) {
        if (delta == 0) {
            return this;
        }
        return with(corner, axis, position(corner, axis) + delta);
    }

    /** Place one corner's coordinate on {@code axis}, clamped to the cell. */
    public CornerShape with(Corner corner, Axis axis, int value) {
        int clamped = clamp(value);
        int i = corner.index() * 3 + axis.ordinal();
        if (data[i] == clamped) {
            return this;
        }
        int[] copy = data.clone();
        copy[i] = clamped;
        return new CornerShape(copy);
    }

    /** Place one corner at an exact grid position (each coordinate clamped). */
    public CornerShape with(Corner corner, Vec3i16 pos) {
        int[] copy = data.clone();
        int base = corner.index() * 3;
        copy[base] = clamp(pos.x());
        copy[base + 1] = clamp(pos.y());
        copy[base + 2] = clamp(pos.z());
        return Arrays.equals(copy, data) ? this : new CornerShape(copy);
    }

    /**
     * Move several corners together by the same delta.
     *
     * <p>Clamping is applied to the <em>group</em>: the delta is reduced so that no
     * member leaves the cell, and then applied to all members. Clamping members
     * one by one would distort the group's shape, which is not what a user
     * dragging four points expects.
     */
    public CornerShape moveGroup(Iterable<Corner> corners, Axis axis, int delta) {
        if (delta == 0) {
            return this;
        }
        int allowed = delta;
        for (Corner c : corners) {
            int v = position(c, axis);
            if (delta > 0) {
                allowed = Math.min(allowed, MAX - v);
            } else {
                allowed = Math.max(allowed, MIN - v);
            }
        }
        if (allowed == 0) {
            return this;
        }
        int[] copy = data.clone();
        for (Corner c : corners) {
            copy[c.index() * 3 + axis.ordinal()] += allowed;
        }
        return new CornerShape(copy);
    }

    /** Reset one corner to its rest position. */
    public CornerShape reset(Corner corner) {
        return with(corner, new Vec3i16(
                corner.restPosition(Axis.X), corner.restPosition(Axis.Y), corner.restPosition(Axis.Z)));
    }

    private static int clamp(int v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    // --- serialisation -----------------------------------------------------------

    public byte[] toBytes() {
        byte[] out = new byte[BYTES];
        for (int i = 0; i < BYTES; i++) {
            out[i] = (byte) data[i];
        }
        return out;
    }

    /**
     * Parse the 24-byte form. Out-of-range values are clamped rather than rejected
     * so that a future relaxation of {@link #MIN}/{@link #MAX} stays backwards
     * compatible in both directions.
     *
     * @throws IllegalArgumentException if the array has the wrong length
     */
    public static CornerShape fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != BYTES) {
            throw new IllegalArgumentException("CornerShape needs exactly " + BYTES + " bytes");
        }
        int[] d = new int[BYTES];
        for (int i = 0; i < BYTES; i++) {
            d[i] = clamp(bytes[i]);
        }
        CornerShape shape = new CornerShape(d);
        return shape.equals(CUBE) ? CUBE : shape;
    }

    // --- object ----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        return o instanceof CornerShape other && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CornerShape{");
        for (Corner c : Corner.values()) {
            if (c.index() > 0) sb.append(", ");
            Vec3i16 p = position(c);
            sb.append(c.index()).append('=').append('(').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(')');
        }
        return sb.append('}').toString();
    }
}