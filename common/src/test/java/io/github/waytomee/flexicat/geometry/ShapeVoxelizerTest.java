package io.github.waytomee.flexicat.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShapeVoxelizerTest {

    private static final int R = ShapeVoxelizer.RESOLUTION;

    /** Rasterise boxes back into cells so unions can be compared exactly. */
    private static boolean[] raster(List<ShapeVoxelizer.Box> boxes) {
        boolean[] cells = new boolean[R * R * R];
        for (ShapeVoxelizer.Box b : boxes) {
            for (int z = b.minZ(); z < b.maxZ(); z++) {
                for (int y = b.minY(); y < b.maxY(); y++) {
                    for (int x = b.minX(); x < b.maxX(); x++) {
                        int i = (z * R + y) * R + x;
                        assertFalse(cells[i], "boxes must not overlap");
                        cells[i] = true;
                    }
                }
            }
        }
        return cells;
    }

    private static CornerShape wedge() {
        // Top north edge pushed down to the floor: a ramp rising towards south.
        return CornerShape.cube()
                .with(Corner.UP_WEST_NORTH, Axis.Y, 0)
                .with(Corner.UP_EAST_NORTH, Axis.Y, 0);
    }

    @Test
    void cubeIsOneFullBox() {
        List<ShapeVoxelizer.Box> boxes = ShapeVoxelizer.voxelize(CornerShape.cube());
        assertEquals(List.of(new ShapeVoxelizer.Box(0, 0, 0, R, R, R)), boxes);
    }

    @Test
    void flatShapeHasNoVolume() {
        CornerShape flat = CornerShape.cube();
        for (Corner c : Corner.values()) {
            if (c.isMax(Axis.Y)) {
                flat = flat.with(c, Axis.Y, 0);
            }
        }
        assertTrue(flat.isFlat());
        assertTrue(ShapeVoxelizer.voxelize(flat).isEmpty());
    }

    @Test
    void halfSlabIsExactlyTheLowerHalf() {
        CornerShape slab = CornerShape.cube();
        for (Corner c : Corner.values()) {
            if (c.isMax(Axis.Y)) {
                slab = slab.with(c, Axis.Y, 8);
            }
        }
        assertEquals(List.of(new ShapeVoxelizer.Box(0, 0, 0, R, 8, R)), ShapeVoxelizer.voxelize(slab));
    }

    @Test
    void wedgeBecomesAStaircaseUnderTheSlope() {
        List<ShapeVoxelizer.Box> boxes = ShapeVoxelizer.voxelize(wedge());
        boolean[] cells = raster(boxes);
        // Surface is y = z (grid units); a cell centre (y+.5, z+.5) is below it iff y < z.
        for (int z = 0; z < R; z++) {
            for (int y = 0; y < R; y++) {
                for (int x = 0; x < R; x++) {
                    assertEquals(y < z, cells[(z * R + y) * R + x], "cell " + x + "," + y + "," + z);
                }
            }
        }
        assertTrue(boxes.size() <= R, "one box per step at most, got " + boxes.size());
    }

    @Test
    void containsFollowsTheSurface() {
        CornerShape w = wedge();
        assertTrue(ShapeVoxelizer.contains(w, 8, 0.5, 15.5));   // deep under the high end
        assertFalse(ShapeVoxelizer.contains(w, 8, 15.5, 0.5));  // above the low end
        assertTrue(ShapeVoxelizer.contains(w, 8, 4, 12));       // below the slope
        assertFalse(ShapeVoxelizer.contains(w, 8, 12, 4));      // above the slope
        assertTrue(ShapeVoxelizer.contains(CornerShape.cube(), 8, 8, 8));
        assertFalse(ShapeVoxelizer.contains(CornerShape.cube(), -1, 8, 8));
    }

    @Test
    void concaveShapeIsHandledAndSmallerThanCube() {
        // One bottom corner pulled into the middle of the cell: non-planar faces, concave hull.
        CornerShape dented = CornerShape.cube().with(Corner.DOWN_WEST_NORTH, new Vec3i16(8, 8, 8));
        List<ShapeVoxelizer.Box> boxes = ShapeVoxelizer.voxelize(dented);
        assertFalse(boxes.isEmpty());
        int volume = boxes.stream().mapToInt(ShapeVoxelizer.Box::volume).sum();
        assertTrue(volume < R * R * R && volume > R * R * R / 2, "volume " + volume);
        raster(boxes); // asserts disjointness
        assertFalse(ShapeVoxelizer.contains(dented, 0.5, 0.5, 0.5));   // the dent
        assertTrue(ShapeVoxelizer.contains(dented, 15.5, 15.5, 15.5)); // untouched corner
    }

    @Test
    void coarseCellsGiveFewerTallerStepsWithinOneCell() {
        CornerShape w = wedge();
        List<ShapeVoxelizer.Box> coarse = ShapeVoxelizer.voxelize(w, 4);
        assertFalse(coarse.isEmpty());
        boolean[] fine = raster(ShapeVoxelizer.voxelize(w));
        boolean[] cells = raster(coarse);
        for (ShapeVoxelizer.Box b : coarse) {
            assertEquals(0, b.minX() % 4);
            assertEquals(0, b.minY() % 4);
            assertEquals(0, b.minZ() % 4);
            assertEquals(0, b.maxX() % 4);
            assertEquals(0, b.maxY() % 4);
            assertEquals(0, b.maxZ() % 4);
        }
        // Coarse cell centres sit at 2, 6, 10, 14: filled iff y < z on the 4-grid, so the
        // ramp becomes three steps of 4/16 instead of fifteen of 1/16.
        assertTrue(coarse.size() <= 4, "expected at most one box per coarse step, got " + coarse.size());
        // A coarse column covers four fine columns of a 45° ramp, so its single top can be
        // off from any one fine top by at most one coarse cell (4 grid units).
        for (int z = 0; z < R; z++) {
            for (int x = 0; x < R; x++) {
                int fineTop = 0, coarseTop = 0;
                for (int y = 0; y < R; y++) {
                    if (fine[(z * R + y) * R + x]) fineTop = y + 1;
                    if (cells[(z * R + y) * R + x]) coarseTop = y + 1;
                }
                assertTrue(Math.abs(fineTop - coarseTop) <= 4, "column " + x + "," + z + ": fine " + fineTop + " coarse " + coarseTop);
            }
        }
    }

    @Test
    void coarseCubeAndSlabAreExact() {
        assertEquals(List.of(new ShapeVoxelizer.Box(0, 0, 0, R, R, R)), ShapeVoxelizer.voxelize(CornerShape.cube(), 4));
        CornerShape slab = CornerShape.cube();
        for (Corner c : Corner.values()) {
            if (c.isMax(Axis.Y)) {
                slab = slab.with(c, Axis.Y, 8);
            }
        }
        assertEquals(List.of(new ShapeVoxelizer.Box(0, 0, 0, R, 8, R)), ShapeVoxelizer.voxelize(slab, 4));
        CornerShape s = slab;
        assertThrows(IllegalArgumentException.class, () -> ShapeVoxelizer.voxelize(s, 3));
        assertThrows(IllegalArgumentException.class, () -> ShapeVoxelizer.voxelize(s, 0));
    }

    @Test
    void boxesStayInsideTheCellAndAreDeterministic() {
        CornerShape shape = CornerShape.cube()
                .with(Corner.UP_EAST_SOUTH, new Vec3i16(3, 11, 14))
                .with(Corner.DOWN_WEST_SOUTH, Axis.Z, 5);
        List<ShapeVoxelizer.Box> a = ShapeVoxelizer.voxelize(shape);
        List<ShapeVoxelizer.Box> b = ShapeVoxelizer.voxelize(shape);
        assertEquals(a, b);
        for (ShapeVoxelizer.Box box : a) {
            assertTrue(box.minX() >= 0 && box.minY() >= 0 && box.minZ() >= 0);
            assertTrue(box.maxX() <= R && box.maxY() <= R && box.maxZ() <= R);
            assertTrue(box.volume() > 0);
        }
    }
}