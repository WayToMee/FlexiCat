package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.Vec3i16;

import java.util.Optional;

/**
 * Finds the corner handle a ray points at.
 *
 * <p>Pure geometry so it can be unit-tested on a plain JVM. Coordinates are in
 * block units relative to the block's minimum corner (so the undeformed cube
 * spans {@code [0, 1]} on each axis). Each handle is an axis-aligned box of
 * half-size {@code halfSize} centred on the corner's position. The nearest hit
 * along the ray wins.
 */
public final class HandlePicker {

    /** Handle half-size used by the client overlay: 1.5 grid units. */
    public static final double DEFAULT_HALF_SIZE = 1.5 / CornerShape.GRID;

    private static final double EPSILON = 1.0e-9;

    private HandlePicker() {
    }

    /** A picked handle and the distance along the (normalised) ray to it. */
    public record Hit(Corner corner, double distance) {
    }

    /**
     * @param shape       the shape whose handles are tested
     * @param ox          ray origin, block-local
     * @param dx          ray direction (need not be normalised, must not be zero)
     * @param halfSize    handle half-size in block units
     * @param maxDistance ignore hits farther than this along the normalised ray
     */
    public static Optional<Hit> pick(CornerShape shape,
                                     double ox, double oy, double oz,
                                     double dx, double dy, double dz,
                                     double halfSize, double maxDistance) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < EPSILON) {
            return Optional.empty();
        }
        dx /= len;
        dy /= len;
        dz /= len;

        Corner best = null;
        double bestT = Double.POSITIVE_INFINITY;
        for (Corner corner : Corner.values()) {
            Vec3i16 p = shape.position(corner);
            double cx = p.x() / (double) CornerShape.GRID;
            double cy = p.y() / (double) CornerShape.GRID;
            double cz = p.z() / (double) CornerShape.GRID;
            double t = intersect(ox, oy, oz, dx, dy, dz,
                    cx - halfSize, cy - halfSize, cz - halfSize,
                    cx + halfSize, cy + halfSize, cz + halfSize, maxDistance);
            if (!Double.isNaN(t) && t < bestT) {
                bestT = t;
                best = corner;
            }
        }
        return best == null ? Optional.empty() : Optional.of(new Hit(best, bestT));
    }

    /**
     * Ray/box slab test. Returns the entry distance (0 if the origin is inside the
     * box) or {@code NaN} if the ray misses the box within {@code [0, maxDistance]}.
     */
    static double intersect(double ox, double oy, double oz,
                            double dx, double dy, double dz,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ,
                            double maxDistance) {
        double tMin = 0.0;
        double tMax = maxDistance;

        double[] o = {ox, oy, oz};
        double[] d = {dx, dy, dz};
        double[] lo = {minX, minY, minZ};
        double[] hi = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < EPSILON) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return Double.NaN;
                }
                continue;
            }
            double t1 = (lo[i] - o[i]) / d[i];
            double t2 = (hi[i] - o[i]) / d[i];
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.NaN;
            }
        }
        return tMin;
    }
}