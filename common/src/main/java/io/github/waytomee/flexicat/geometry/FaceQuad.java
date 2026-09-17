package io.github.waytomee.flexicat.geometry;

/**
 * The four corner positions of one face, in the face's fixed counter-clockwise
 * order, plus the geometric facts other systems need from it.
 *
 * <p>Eight corner positions do not fully define a non-planar quad: the surface
 * depends on which diagonal splits it. FlexiCat splits along the diagonal that passes
 * through the corner lying farthest from the plane of the other three
 * ({@link #splitStart()}). A single moved corner therefore always folds the face
 * through itself — both triangles slope — instead of leaving half the face flat with a
 * crease across the middle. Ties (planar faces, symmetric saddles) fall back to the
 * first–third diagonal. The rule depends only on the positions, so the same eight
 * points always produce the same surface, including under mirroring.
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

    /** Twice the area vector of triangle {@code (vertex(i), vertex(j), vertex(k))}. */
    public Vec3i16 triangleNormal(int i, int j, int k) {
        Vec3i16 p = vertex(i);
        return vertex(j).sub(p).cross(vertex(k).sub(p));
    }

    /**
     * Index of the vertex the split diagonal starts at: {@code 0} splits along
     * {@code a–c} (triangles {@code a,b,c} and {@code a,c,d}), {@code 1} splits along
     * {@code b–d} (triangles {@code b,c,d} and {@code b,d,a}).
     *
     * <p>The four corners span a tetrahedron; each corner's distance from the plane of
     * the other three is {@code 3·volume / area(opposite triangle)}. The volume is
     * shared, so the farthest corner is the one with the <em>smallest</em> opposite
     * triangle, and the diagonal is the one that contains it. Planar faces and exact
     * ties keep {@code 0}.
     */
    public int splitStart() {
        if (isPlanar()) {
            return 0;
        }
        long oppositeA = triangleNormal(1, 2, 3).lengthSquared(); // b, c, d
        long oppositeB = triangleNormal(0, 2, 3).lengthSquared(); // a, c, d
        long oppositeC = triangleNormal(0, 1, 3).lengthSquared(); // a, b, d
        long oppositeD = triangleNormal(0, 1, 2).lengthSquared(); // a, b, c
        return Math.min(oppositeA, oppositeC) <= Math.min(oppositeB, oppositeD) ? 0 : 1;
    }

    /** Twice the area vector of the first triangle of the split: {@code (s, s+1, s+2)}. */
    public Vec3i16 normal1() {
        int s = splitStart();
        return triangleNormal(s, s + 1, s + 2);
    }

    /** Twice the area vector of the second triangle of the split: {@code (s, s+2, s+3)}. */
    public Vec3i16 normal2() {
        int s = splitStart();
        return triangleNormal(s, s + 2, s + 3);
    }

    /** Twice the face's area vector — the sum over its two triangles, independent of the split. */
    public Vec3i16 areaNormal() {
        return triangleNormal(0, 1, 2).add(triangleNormal(0, 2, 3));
    }

    /** Whether all four vertices lie in one plane. */
    public boolean isPlanar() {
        // Planar iff d lies in the plane of (a, b, c) — or (a, b, c) are collinear, in
        // which case any fourth point is coplanar with them.
        Vec3i16 n = triangleNormal(0, 1, 2);
        if (n.isZero()) {
            return true;
        }
        return n.dot(d.sub(a)) == 0;
    }

    /**
     * Whether the face encloses no area: collapsed to a line or point, or folded back
     * onto itself so its two triangles cancel (two opposite corners coincide).
     */
    public boolean isDegenerate() {
        return areaNormal().isZero();
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