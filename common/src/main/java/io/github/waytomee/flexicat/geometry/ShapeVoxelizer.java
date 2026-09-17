package io.github.waytomee.flexicat.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Approximates the volume enclosed by a {@link CornerShape} with axis-aligned boxes
 * on the shape's own 1/16 grid — the input Minecraft's collision and picking need,
 * since a {@code VoxelShape} can only be made of boxes.
 *
 * <p>Pure geometry, no Minecraft classes. The algorithm:
 * <ol>
 *   <li>The twelve triangles of the shape (each face split along its contract
 *       diagonal, degenerate triangles dropped) form a closed surface, even when
 *       faces are non-planar or the shape is concave.</li>
 *   <li>Every grid cell is classified by its centre: a ray from the centre is
 *       intersected with the twelve triangles and an odd number of crossings means
 *       "inside". Centre sampling keeps the approximation tight (never fatter than
 *       one cell) and monotone: pulling a corner in never adds collision.</li>
 *   <li>Filled cells are merged greedily (x, then y, then z) into as few boxes as the
 *       scan order allows. The result is deterministic for a given shape.</li>
 * </ol>
 *
 * <p>The cell size is a parameter: {@code 1} gives the exact 1/16 staircase used for
 * picking; a coarser size (e.g. {@code 4}) gives fewer, taller steps, which is what
 * entity movement wants (see {@link #voxelize(CornerShape, int)}).
 *
 * <p>Shapes with no volume (flat plates, zero-thickness slivers) yield an empty list;
 * callers substitute a thin fallback so the block stays targetable.
 */
public final class ShapeVoxelizer {

    /** Number of sample cells per axis at full resolution; equals the corner grid. */
    public static final int RESOLUTION = CornerShape.GRID;

    /**
     * Ray direction for the inside test, in grid units. Deliberately not axis-aligned
     * and not a "nice" ratio, so a ray from a cell centre (half-integer coordinates)
     * practically never runs exactly through a triangle edge or vertex (integer
     * coordinates), which would double-count a crossing.
     */
    private static final double DIR_X = 1.0;
    private static final double DIR_Y = 0.0137;
    private static final double DIR_Z = 0.0071;

    private static final List<Box> FULL = List.of(new Box(0, 0, 0, RESOLUTION, RESOLUTION, RESOLUTION));

    /** One axis-aligned box in grid units, {@code min} inclusive, {@code max} exclusive. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public int volume() {
            return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
        }

        Box scaled(int factor) {
            return new Box(minX * factor, minY * factor, minZ * factor, maxX * factor, maxY * factor, maxZ * factor);
        }
    }

    private ShapeVoxelizer() {
    }

    /** Boxes covering the shape's volume on the 1/16 grid, or an empty list for a shape without volume. */
    public static List<Box> voxelize(CornerShape shape) {
        return voxelize(shape, 1);
    }

    /**
     * Boxes covering the shape's volume with cells of {@code cell} grid units
     * ({@code cell} must divide {@link #RESOLUTION}). Each cell is classified by its
     * centre, so a coarse cell can be off by up to half its size either way; box
     * coordinates (in grid units) are multiples of {@code cell}.
     */
    public static List<Box> voxelize(CornerShape shape, int cell) {
        if (cell < 1 || RESOLUTION % cell != 0) {
            throw new IllegalArgumentException("cell size must divide " + RESOLUTION + ": " + cell);
        }
        if (shape.isCube()) {
            return FULL;
        }
        if (shape.isFlat()) {
            return Collections.emptyList();
        }
        double[][] tris = triangles(shape);
        if (tris.length == 0) {
            return Collections.emptyList();
        }
        int n = RESOLUTION / cell;
        boolean[] filled = new boolean[n * n * n];
        int[] b = shape.bounds();
        boolean any = false;
        // Only cells overlapping the corner bounds can be inside the shape.
        int x0 = b[0] / cell, x1 = ceilDiv(b[1], cell);
        int y0 = b[2] / cell, y1 = ceilDiv(b[3], cell);
        int z0 = b[4] / cell, z1 = ceilDiv(b[5], cell);
        for (int z = z0; z < z1; z++) {
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if (contains(tris, (x + 0.5) * cell, (y + 0.5) * cell, (z + 0.5) * cell)) {
                        filled[index(x, y, z, n)] = true;
                        any = true;
                    }
                }
            }
        }
        if (!any) {
            return Collections.emptyList();
        }
        List<Box> boxes = merge(filled, n);
        if (cell == 1) {
            return boxes;
        }
        List<Box> scaled = new ArrayList<>(boxes.size());
        for (Box box : boxes) {
            scaled.add(box.scaled(cell));
        }
        return scaled;
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }

    /**
     * Whether a point (grid units) lies inside the shape's closed surface.
     * Points exactly on the surface are not guaranteed either way.
     */
    public static boolean contains(CornerShape shape, double x, double y, double z) {
        if (shape.isCube()) {
            return x > 0 && x < RESOLUTION && y > 0 && y < RESOLUTION && z > 0 && z < RESOLUTION;
        }
        return contains(triangles(shape), x, y, z);
    }

    // --- surface ------------------------------------------------------------------

    /** The shape's triangles as {@code {ax,ay,az, bx,by,bz, cx,cy,cz}}, degenerate ones dropped. */
    static double[][] triangles(CornerShape shape) {
        List<double[]> out = new ArrayList<>(12);
        for (CubeFace face : CubeFace.values()) {
            FaceQuad q = shape.face(face);
            if (q.isDegenerate()) {
                continue;
            }
            // Contract 5: split along the diagonal starting at splitStart() — the same
            // two triangles the renderer draws.
            int s = q.splitStart();
            if (!q.normal1().isZero()) {
                out.add(tri(q.vertex(s), q.vertex(s + 1), q.vertex(s + 2)));
            }
            if (!q.normal2().isZero()) {
                out.add(tri(q.vertex(s), q.vertex(s + 2), q.vertex(s + 3)));
            }
        }
        return out.toArray(new double[0][]);
    }

    private static double[] tri(Vec3i16 a, Vec3i16 b, Vec3i16 c) {
        return new double[] {a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), c.x(), c.y(), c.z()};
    }

    /** Ray-parity inside test (Möller–Trumbore against every triangle). */
    private static boolean contains(double[][] tris, double px, double py, double pz) {
        int crossings = 0;
        for (double[] t : tris) {
            double e1x = t[3] - t[0], e1y = t[4] - t[1], e1z = t[5] - t[2];
            double e2x = t[6] - t[0], e2y = t[7] - t[1], e2z = t[8] - t[2];
            // h = dir × e2
            double hx = DIR_Y * e2z - DIR_Z * e2y;
            double hy = DIR_Z * e2x - DIR_X * e2z;
            double hz = DIR_X * e2y - DIR_Y * e2x;
            double a = e1x * hx + e1y * hy + e1z * hz;
            if (Math.abs(a) < 1.0e-12) {
                continue; // ray parallel to the triangle plane
            }
            double f = 1.0 / a;
            double sx = px - t[0], sy = py - t[1], sz = pz - t[2];
            double u = f * (sx * hx + sy * hy + sz * hz);
            if (u < 0.0 || u > 1.0) {
                continue;
            }
            // q = s × e1
            double qx = sy * e1z - sz * e1y;
            double qy = sz * e1x - sx * e1z;
            double qz = sx * e1y - sy * e1x;
            double v = f * (DIR_X * qx + DIR_Y * qy + DIR_Z * qz);
            if (v < 0.0 || u + v > 1.0) {
                continue;
            }
            double dist = f * (e2x * qx + e2y * qy + e2z * qz);
            if (dist > 1.0e-9) {
                crossings++;
            }
        }
        return (crossings & 1) == 1;
    }

    // --- merging -------------------------------------------------------------------

    private static int index(int x, int y, int z, int n) {
        return (z * n + y) * n + x;
    }

    /** Greedy run-length merge on an {@code n³} grid: extend along x, then grow the row along y, then the slab along z. */
    static List<Box> merge(boolean[] filled, int n) {
        boolean[] used = new boolean[filled.length];
        List<Box> boxes = new ArrayList<>();
        for (int z = 0; z < n; z++) {
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    int i = index(x, y, z, n);
                    if (!filled[i] || used[i]) {
                        continue;
                    }
                    int x2 = x;
                    while (x2 + 1 < n && free(filled, used, x2 + 1, y, z, n)) {
                        x2++;
                    }
                    int y2 = y;
                    while (y2 + 1 < n && rowFree(filled, used, x, x2, y2 + 1, z, n)) {
                        y2++;
                    }
                    int z2 = z;
                    while (z2 + 1 < n && slabFree(filled, used, x, x2, y, y2, z2 + 1, n)) {
                        z2++;
                    }
                    for (int zz = z; zz <= z2; zz++) {
                        for (int yy = y; yy <= y2; yy++) {
                            for (int xx = x; xx <= x2; xx++) {
                                used[index(xx, yy, zz, n)] = true;
                            }
                        }
                    }
                    boxes.add(new Box(x, y, z, x2 + 1, y2 + 1, z2 + 1));
                }
            }
        }
        return boxes;
    }

    private static boolean free(boolean[] filled, boolean[] used, int x, int y, int z, int n) {
        int i = index(x, y, z, n);
        return filled[i] && !used[i];
    }

    private static boolean rowFree(boolean[] filled, boolean[] used, int x1, int x2, int y, int z, int n) {
        for (int x = x1; x <= x2; x++) {
            if (!free(filled, used, x, y, z, n)) {
                return false;
            }
        }
        return true;
    }

    private static boolean slabFree(boolean[] filled, boolean[] used, int x1, int x2, int y1, int y2, int z, int n) {
        for (int y = y1; y <= y2; y++) {
            if (!rowFree(filled, used, x1, x2, y, z, n)) {
                return false;
            }
        }
        return true;
    }
}