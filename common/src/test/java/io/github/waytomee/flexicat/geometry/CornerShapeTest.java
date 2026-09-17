package io.github.waytomee.flexicat.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CornerShapeTest {

    @Test
    void cubeIsCube() {
        CornerShape cube = CornerShape.cube();
        assertTrue(cube.isCube());
        assertFalse(cube.isFlat());
        assertArrayEquals(new int[] {0, 16, 0, 16, 0, 16}, cube.bounds());
        for (Corner c : Corner.values()) {
            assertEquals(new Vec3i16(0, 0, 0), cube.offset(c));
        }
    }

    @Test
    void cornerIndexBitsMatchAxes() {
        assertEquals(Corner.byIndex(0b101), Corner.of(true, false, true));
        assertTrue(Corner.UP_EAST_SOUTH.isMax(Axis.X));
        assertTrue(Corner.UP_EAST_SOUTH.isMax(Axis.Y));
        assertTrue(Corner.UP_EAST_SOUTH.isMax(Axis.Z));
        assertFalse(Corner.DOWN_WEST_NORTH.isMax(Axis.Y));
        assertEquals(Corner.DOWN_EAST_NORTH, Corner.UP_EAST_NORTH.across(Axis.Y));
    }

    @Test
    void userExampleLowerTopCornerByHalfBlock() {
        // "опустил верхнюю угловую точку на половину блока (на 8 вниз)"
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8);
        assertEquals(8, s.position(Corner.UP_EAST_NORTH, Axis.Y));
        assertEquals(new Vec3i16(0, -8, 0), s.offset(Corner.UP_EAST_NORTH));
        assertFalse(s.isCube());
        // untouched corners keep their rest positions
        assertEquals(16, s.position(Corner.UP_WEST_NORTH, Axis.Y));
    }

    @Test
    void twoLoweredCornersMakeARampLikeShape() {
        // "Сделал так с двумя точками, и у тебя получилась почти рампа."
        CornerShape s = CornerShape.cube()
                .move(Corner.UP_EAST_NORTH, Axis.Y, -16)
                .move(Corner.UP_EAST_SOUTH, Axis.Y, -16);
        FaceQuad east = s.face(CubeFace.EAST);
        // east face collapsed to a line -> degenerate, top face is a planar slope
        assertTrue(east.isDegenerate());
        FaceQuad top = s.face(CubeFace.UP);
        assertTrue(top.isPlanar());
        assertFalse(top.isAxisAligned());
        assertFalse(top.isDegenerate());
    }

    @Test
    void movesAreClampedToTheCell() {
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -100);
        assertEquals(CornerShape.MIN, s.position(Corner.UP_EAST_NORTH, Axis.Y));
        s = s.move(Corner.UP_EAST_NORTH, Axis.Y, +100);
        assertEquals(CornerShape.MAX, s.position(Corner.UP_EAST_NORTH, Axis.Y));
    }

    @Test
    void groupMoveIsClampedAsAWhole() {
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -4);
        // group of two: one at 12, one at 16; moving up by +8 must be limited to 0 (16 is already max)
        CornerShape moved = s.moveGroup(List.of(Corner.UP_EAST_NORTH, Corner.UP_WEST_NORTH), Axis.Y, +8);
        assertSame(s, moved, "no member may leave the cell, so nothing moves");
        // moving down by -20: limited by the corner at 12 -> allowed -12
        moved = s.moveGroup(List.of(Corner.UP_EAST_NORTH, Corner.UP_WEST_NORTH), Axis.Y, -20);
        assertEquals(0, moved.position(Corner.UP_EAST_NORTH, Axis.Y));
        assertEquals(4, moved.position(Corner.UP_WEST_NORTH, Axis.Y));
    }

    @Test
    void countsMovedCorners() {
        assertEquals(0, CornerShape.cube().movedCorners());
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8).move(Corner.UP_WEST_NORTH, Axis.Y, -8);
        assertEquals(2, s.movedCorners());
        assertEquals(1, s.reset(Corner.UP_WEST_NORTH).movedCorners());
    }

    @Test
    void coincidentCornersStayDistinct() {
        CornerShape s = CornerShape.cube().with(Corner.UP_EAST_NORTH, new Vec3i16(16, 0, 0));
        assertEquals(s.position(Corner.DOWN_EAST_NORTH), s.position(Corner.UP_EAST_NORTH));
        CornerShape back = s.reset(Corner.UP_EAST_NORTH);
        assertTrue(back.isCube(), "the moved corner can be separated again");
    }

    @Test
    void flatShapeIsDetected() {
        CornerShape s = CornerShape.cube();
        for (Corner c : Corner.values()) {
            if (c.isMax(Axis.Y)) {
                s = s.move(c, Axis.Y, -16);
            }
        }
        assertTrue(s.isFlat());
    }

    @Test
    void byteRoundTrip() {
        CornerShape s = CornerShape.cube()
                .move(Corner.UP_EAST_NORTH, Axis.Y, -8)
                .move(Corner.DOWN_WEST_SOUTH, Axis.X, 3)
                .move(Corner.UP_WEST_SOUTH, Axis.Z, -5);
        byte[] bytes = s.toBytes();
        assertEquals(CornerShape.BYTES, bytes.length);
        assertEquals(s, CornerShape.fromBytes(bytes));
        assertSame(CornerShape.cube(), CornerShape.fromBytes(CornerShape.cube().toBytes()));
        assertThrows(IllegalArgumentException.class, () -> CornerShape.fromBytes(new byte[3]));
    }

    @Test
    void fromBytesClampsOutOfRangeValues() {
        byte[] bytes = CornerShape.cube().toBytes();
        bytes[Corner.UP_EAST_NORTH.index() * 3 + Axis.Y.ordinal()] = (byte) 120;
        bytes[Corner.DOWN_WEST_NORTH.index() * 3 + Axis.X.ordinal()] = (byte) -7;
        CornerShape s = CornerShape.fromBytes(bytes);
        assertEquals(16, s.position(Corner.UP_EAST_NORTH, Axis.Y));
        assertEquals(0, s.position(Corner.DOWN_WEST_NORTH, Axis.X));
    }

    @Test
    void cubeFacesAreFullAndOutwardFacing() {
        CornerShape cube = CornerShape.cube();
        for (CubeFace f : CubeFace.values()) {
            FaceQuad q = cube.face(f);
            assertTrue(q.isFullCubeFace(), f + " should be a full cube face");
            Vec3i16 n = q.normal1();
            int sign = f.isMax() ? 1 : -1;
            assertTrue(n.get(f.axis()) * sign > 0, f + " normal must point outwards, got " + n);
            for (Corner c : f.corners()) {
                assertTrue(f.contains(c));
            }
            assertEquals(f, CubeFace.of(f.axis(), f.isMax()));
            assertEquals(f.axis(), f.opposite().axis());
            assertNotEquals(f.isMax(), f.opposite().isMax());
        }
    }

    @Test
    void mirrorIsAnInvolutionAndKeepsTheCube() {
        assertSame(CornerShape.cube(), CornerShape.cube().mirror(Axis.X));
        CornerShape s = CornerShape.cube()
                .move(Corner.UP_EAST_NORTH, Axis.Y, -8)
                .move(Corner.DOWN_WEST_SOUTH, Axis.X, 3);
        for (Axis a : Axis.values()) {
            assertNotEquals(s, s.mirror(a), "mirroring across " + a + " must change this shape");
            assertEquals(s, s.mirror(a).mirror(a), "mirror twice across " + a + " is identity");
        }
    }

    @Test
    void mirrorMovesTheDisplacementToTheOtherSide() {
        // lowered north-east top corner, mirrored across X → lowered north-west top corner
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8);
        CornerShape m = s.mirror(Axis.X);
        assertEquals(8, m.position(Corner.UP_WEST_NORTH, Axis.Y));
        assertEquals(16, m.position(Corner.UP_EAST_NORTH, Axis.Y));
        assertEquals(1, m.movedCorners());
        // an X displacement flips sign across X
        CornerShape t = CornerShape.cube().move(Corner.DOWN_WEST_SOUTH, Axis.X, 3);
        assertEquals(13, t.mirror(Axis.X).position(Corner.DOWN_EAST_SOUTH, Axis.X));
    }

    @Test
    void rotateYFourTimesIsIdentityAndCubeIsInvariant() {
        assertSame(CornerShape.cube(), CornerShape.cube().rotateY(1));
        CornerShape s = CornerShape.cube()
                .move(Corner.UP_EAST_NORTH, Axis.Y, -8)
                .move(Corner.DOWN_WEST_SOUTH, Axis.X, 3);
        assertEquals(s, s.rotateY(4));
        assertEquals(s, s.rotateY(1).rotateY(3));
        assertEquals(s.rotateY(3), s.rotateY(-1));
        assertNotEquals(s, s.rotateY(1));
        assertEquals(s.rotateY(2), s.rotateY(1).rotateY(1));
    }

    @Test
    void rotateYClockwiseTurnsNorthToEast() {
        // north = -Z, east = +X. Seen from above, clockwise: north → east → south → west.
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8);
        CornerShape r = s.rotateY(1);
        assertEquals(8, r.position(Corner.UP_EAST_SOUTH, Axis.Y), "NE corner rotates to SE");
        assertEquals(1, r.movedCorners());
        // a +X displacement becomes a +Z displacement
        CornerShape t = CornerShape.cube().move(Corner.DOWN_WEST_NORTH, Axis.X, 3);
        CornerShape tr = t.rotateY(1);
        assertEquals(3, tr.position(Corner.DOWN_EAST_NORTH, Axis.Z), "NW corner rotates to NE, X shift becomes Z shift");
        assertEquals(16, tr.position(Corner.DOWN_EAST_NORTH, Axis.X));
    }

    @Test
    void nonPlanarQuadIsDetected() {
        // pull one top corner down: the top face is no longer planar
        CornerShape s = CornerShape.cube().move(Corner.UP_EAST_NORTH, Axis.Y, -8);
        assertFalse(s.face(CubeFace.UP).isPlanar());
        assertFalse(s.face(CubeFace.UP).isFullCubeFace());
        // the bottom face is untouched
        assertTrue(s.face(CubeFace.DOWN).isFullCubeFace());
    }
}