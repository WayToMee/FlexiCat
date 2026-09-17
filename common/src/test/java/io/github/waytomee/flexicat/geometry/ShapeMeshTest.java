package io.github.waytomee.flexicat.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ShapeMeshTest {

    private static Optional<ShapeMesh.Face> face(List<ShapeMesh.Face> faces, CubeFace f) {
        return faces.stream().filter(x -> x.face() == f).findFirst();
    }

    /** Twice the signed area normal of triangle (a, b, c), from emitted vertices. */
    private static float[] triangleNormal(ShapeMesh.Vertex a, ShapeMesh.Vertex b, ShapeMesh.Vertex c) {
        float ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        float vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
        return new float[] {uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
    }

    /** Twice the signed area normal of the quad's first triangle (v0, v1, v2). */
    private static float[] normal(ShapeMesh.Face f) {
        return triangleNormal(f.v0(), f.v1(), f.v2());
    }

    @Test
    void oneLoweredTopCornerFoldsTheTopThroughIt() {
        // Whichever top corner is lowered, the top must read as one full slope: the fold
        // (the v0–v2 diagonal Minecraft splits along) passes through the moved corner and
        // both triangles tilt. With a fixed first–third diagonal only two of the four
        // corners behaved that way; the other two left half the face flat.
        for (Corner moved : CubeFace.UP.corners()) {
            CornerShape shape = CornerShape.cube().move(moved, Axis.Y, -8);
            ShapeMesh.Face up = face(ShapeMesh.build(shape), CubeFace.UP).orElseThrow();
            assertTrue(up.v0().y() == 0.5f || up.v2().y() == 0.5f, moved + ": fold must pass through the moved corner");
            float[] n1 = triangleNormal(up.v0(), up.v1(), up.v2());
            float[] n2 = triangleNormal(up.v0(), up.v2(), up.v3());
            assertTrue(n1[1] > 0 && n2[1] > 0, moved + ": winding must stay outward");
            assertTrue(n1[0] != 0 || n1[2] != 0, moved + ": first triangle must slope");
            assertTrue(n2[0] != 0 || n2[2] != 0, moved + ": second triangle must slope");
            assertEquals(CubeFace.UP, up.lightFace());
            // The vertex order is a rotation of the face's corner order, never a reshuffle.
            int start = shape.face(CubeFace.UP).splitStart();
            for (int i = 0; i < 4; i++) {
                Vec3i16 p = shape.face(CubeFace.UP).vertex(start + i);
                assertEquals((float) p.xBlocks(), up.vertex(i).x(), 1e-6, moved + ": vertex " + i);
                assertEquals((float) p.zBlocks(), up.vertex(i).z(), 1e-6, moved + ": vertex " + i);
            }
        }
    }

    @Test
    void planarFacesKeepTheFirstThirdDiagonal() {
        CornerShape ramp = CornerShape.cube()
                .move(Corner.UP_EAST_SOUTH, Axis.Y, -12)
                .move(Corner.UP_EAST_NORTH, Axis.Y, -12);
        for (ShapeMesh.Face f : ShapeMesh.build(ramp)) {
            FaceQuad quad = ramp.face(f.face());
            assertTrue(quad.isPlanar(), f.face() + " should be planar");
            assertEquals(0, quad.splitStart());
            assertEquals((float) quad.a().xBlocks(), f.v0().x(), 1e-6);
            assertEquals((float) quad.a().yBlocks(), f.v0().y(), 1e-6);
            assertEquals((float) quad.a().zBlocks(), f.v0().z(), 1e-6);
        }
    }

    @Test
    void splitChoiceIsMirrorConsistent() {
        // Lowering the east or the west south top corner are mirror images; the chosen
        // diagonal must mirror too (fold through the moved corner in both cases).
        FaceQuad east = CornerShape.cube().move(Corner.UP_EAST_SOUTH, Axis.Y, -8).face(CubeFace.UP);
        FaceQuad west = CornerShape.cube().move(Corner.UP_WEST_SOUTH, Axis.Y, -8).face(CubeFace.UP);
        // UP order: WEST_SOUTH(0), EAST_SOUTH(1), EAST_NORTH(2), WEST_NORTH(3)
        assertEquals(1, east.splitStart(), "east-south is corner 1 → b–d diagonal");
        assertEquals(0, west.splitStart(), "west-south is corner 0 → a–c diagonal");
        assertFalse(east.normal1().isZero());
        assertFalse(east.normal2().isZero());
        assertNotEquals(0, east.normal1().x() + east.normal1().z(), "no horizontal triangle left");
        assertNotEquals(0, east.normal2().x() + east.normal2().z(), "no horizontal triangle left");
    }

    @Test
    void coincidingOppositeCornersMakeTheFaceDegenerate() {
        // UP_EAST_SOUTH moved onto UP_WEST_NORTH: the top folds back onto itself and
        // encloses nothing — it must vanish, not render as a one-sided fin.
        CornerShape shape = CornerShape.cube().with(Corner.UP_EAST_SOUTH, new Vec3i16(0, 16, 0));
        FaceQuad up = shape.face(CubeFace.UP);
        assertTrue(up.isDegenerate());
        assertTrue(face(ShapeMesh.build(shape), CubeFace.UP).isEmpty());
    }

    @Test
    void cubeHasSixFullFacesWithOutwardWinding() {
        List<ShapeMesh.Face> faces = ShapeMesh.build(CornerShape.cube());
        assertEquals(6, faces.size());
        for (ShapeMesh.Face f : faces) {
            assertTrue(f.fullCubeFace(), f.face() + " should be a full cube face");
            assertEquals(f.face(), f.lightFace());
            float[] n = normal(f);
            Axis axis = f.face().axis();
            float along = axis == Axis.X ? n[0] : axis == Axis.Y ? n[1] : n[2];
            assertTrue(f.face().isMax() ? along > 0 : along < 0, f.face() + " winding must face outward");
        }
    }

    @Test
    void cubeUvsMatchVanillaProjection() {
        List<ShapeMesh.Face> faces = ShapeMesh.build(CornerShape.cube());
        // UP face starts at UP_WEST_SOUTH (x=0, z=16) → u=0, v=16 with the vanilla projection.
        ShapeMesh.Face up = face(faces, CubeFace.UP).orElseThrow();
        assertEquals(0f, up.v0().u());
        assertEquals(16f, up.v0().v());
        // NORTH face starts at UP_EAST_NORTH (x=16, y=16) → u=0, v=0 (mirrored on north).
        ShapeMesh.Face north = face(faces, CubeFace.NORTH).orElseThrow();
        assertEquals(0f, north.v0().u());
        assertEquals(0f, north.v0().v());
        for (ShapeMesh.Face f : faces) {
            for (int i = 0; i < 4; i++) {
                ShapeMesh.Vertex v = f.vertex(i);
                assertTrue(v.u() == 0f || v.u() == 16f, "cube UVs sit on texture edges");
                assertTrue(v.v() == 0f || v.v() == 16f, "cube UVs sit on texture edges");
            }
        }
    }

    @Test
    void loweredCornerBreaksTouchingFacesOnly() {
        CornerShape shape = CornerShape.cube().move(Corner.UP_EAST_SOUTH, Axis.Y, -8);
        List<ShapeMesh.Face> faces = ShapeMesh.build(shape);
        assertEquals(6, faces.size());
        assertFalse(face(faces, CubeFace.UP).orElseThrow().fullCubeFace());
        assertFalse(face(faces, CubeFace.EAST).orElseThrow().fullCubeFace());
        assertFalse(face(faces, CubeFace.SOUTH).orElseThrow().fullCubeFace());
        assertTrue(face(faces, CubeFace.DOWN).orElseThrow().fullCubeFace());
        assertTrue(face(faces, CubeFace.WEST).orElseThrow().fullCubeFace());
        assertTrue(face(faces, CubeFace.NORTH).orElseThrow().fullCubeFace());
        // Texture follows the moved corner: its v on the UP face is unchanged (u=x, v=z), but on
        // the SOUTH face the corner slid down (v = 16 - y = 8).
        ShapeMesh.Face south = face(faces, CubeFace.SOUTH).orElseThrow();
        assertEquals(8f, south.v3().v(), "UP_EAST_SOUTH is the 4th SOUTH vertex");
    }

    @Test
    void rampKeepsUpAsLightFaceWhileTilted() {
        CornerShape shape = CornerShape.cube()
                .move(Corner.UP_EAST_SOUTH, Axis.Y, -12)
                .move(Corner.UP_EAST_NORTH, Axis.Y, -12);
        ShapeMesh.Face up = face(ShapeMesh.build(shape), CubeFace.UP).orElseThrow();
        assertEquals(CubeFace.UP, up.lightFace());
        assertFalse(up.fullCubeFace());
    }

    @Test
    void steepFaceSwitchesLightFace() {
        // Fold the top down to the east edge: the UP face now stands almost vertical, facing west… no,
        // its normal tilts towards +X/-X? Move the two west top corners all the way down instead:
        // the UP face becomes a slope from the east top edge to the west bottom edge, normal ≈ (-1, +1, 0).
        CornerShape shape = CornerShape.cube()
                .move(Corner.UP_WEST_SOUTH, Axis.Y, -16)
                .move(Corner.UP_WEST_NORTH, Axis.Y, -16);
        ShapeMesh.Face up = face(ShapeMesh.build(shape), CubeFace.UP).orElseThrow();
        // 45° slope: x and y components tie; Y wins the tie so shading stays "top".
        assertEquals(CubeFace.UP, up.lightFace());

        // Push further: move the same corners east too, so the face leans past 45°.
        CornerShape steep = shape
                .move(Corner.UP_WEST_SOUTH, Axis.X, 8)
                .move(Corner.UP_WEST_NORTH, Axis.X, 8);
        ShapeMesh.Face steepUp = face(ShapeMesh.build(steep), CubeFace.UP).orElseThrow();
        assertEquals(CubeFace.WEST, steepUp.lightFace());
    }

    @Test
    void collapsedFacesAreSkipped() {
        CornerShape plate = CornerShape.cube();
        for (Corner c : CubeFace.UP.corners()) {
            plate = plate.move(c, Axis.Y, -16);
        }
        List<ShapeMesh.Face> faces = ShapeMesh.build(plate);
        // Top and bottom coincide (both non-degenerate); the four side faces collapsed to lines.
        assertEquals(2, faces.size());
        assertTrue(face(faces, CubeFace.UP).isPresent());
        assertTrue(face(faces, CubeFace.DOWN).isPresent());
        assertTrue(face(faces, CubeFace.NORTH).isEmpty());
    }

    @Test
    void verticesAreInBlockUnits() {
        CornerShape shape = CornerShape.cube().move(Corner.DOWN_WEST_NORTH, Axis.X, 4);
        ShapeMesh.Face down = face(ShapeMesh.build(shape), CubeFace.DOWN).orElseThrow();
        assertEquals(0.25f, down.v0().x(), 1e-6);
        assertEquals(0f, down.v0().y(), 1e-6);
        assertEquals(0f, down.v0().z(), 1e-6);
        assertEquals(4f, down.v0().u());
    }
}