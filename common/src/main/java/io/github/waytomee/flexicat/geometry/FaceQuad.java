package io.github.waytomee.flexicat.geometry;

/**
 * The four corner positions of one face, in the face's fixed counter-clockwise
 * order, plus the geometric facts other systems need from it.
 *
 * <p>Eight corner positions do not fully define a non-planar quad: the surface
 * depends on which diagonal splits it. FlexiCat always splits along the diagonal
 * {@code corner(0) - corner(2)} (first and third corner of the face's fixed order).
 * That rule is stable across saving, copying and mirroring, so the same eight
 * points always produce the same surface.
 */
public record FaceQuad(CubeFace face, Vec3i16 a, Vec3i16 b, Vec3i16 c, Vec3i16 d) {

    public Vec3i16 vertex(int i) {
        return switch (i & 3) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> d;
        };
    }

    /** Twice the signed area vector of triangle (a, b, c). */
    public Vec3i16 normal1() {
        return b.sub(a).cross(c.sub(a));
    }

    /** Twice the signed area vector of triangle (a, c, d). */
    public Vec3i16 normal2() {
        return c.sub(a).cross(d.sub(a));
    }

    /** Whether all four vertices lie in one plane. */
    public boolean isPlanar() {
        // Planar iff d lies in the plane of (a, b, c) — or the first triangle is
        // itself degenerate, in which case the second triangle defines the plane.
        Vec3i16 n = normal1();
        if (n.isZero()) {
            return true;
        }
        return n.dot(d.sub(a)) == 0;
    }

    /** Whether the face has collapsed to a line or point (both triangles have zero area). */
    public boolean isDegenerate() {
        return normal1().isZero() && normal2().isZero();
    }

    /** Whether the face is a rectangle with all vertices on the same coordinate of its axis. */
    public boolean isAxisAligned() {
        Axis axis = face.axis();
        int v = a.get(axis);
        return b.get(axis) == v && c.get(axis) == v && d.get(axis) == v;
    }

    /**
     * Whether the face is a full, undeformed cube face — i.e. it would cull an
     * identical neighbour face exactly like a vanilla full block would.
     */
    public boolean isFullCubeFace() {
        if (!isAxisAligned()) {
            return false;
        }
        int level = face.isMax() ? CornerShape.MAX : CornerShape.MIN;
        if (a.get(face.axis()) != level) {
            return false;
        }
        for (Corner corner : face.corners()) {
            Vec3i16 expected = new Vec3i16(corner.restPosition(Axis.X), corner.restPosition(Axis.Y), corner.restPosition(Axis.Z));
            // vertices are stored in the same order as face.corners()
            int i = indexOf(corner);
            if (!vertex(i).equals(expected)) {
                return false;
            }
        }
        return true;
    }

    private int indexOf(Corner corner) {
        for (int i = 0; i < 4; i++) {
            if (face.corner(i) == corner) {
                return i;
            }
        }
        throw new IllegalStateException(corner + " is not on face " + face);
    }
}