package io.github.waytomee.flexicat.block;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexiCatShapesTest {

    private static CornerShape wedge() {
        return CornerShape.cube()
                .with(Corner.UP_WEST_NORTH, Axis.Y, 0)
                .with(Corner.UP_EAST_NORTH, Axis.Y, 0);
    }

    @Test
    void cubeIsTheVanillaFullBlock() {
        assertSame(Shapes.block(), FlexiCatShapes.of(CornerShape.cube()));
    }

    @Test
    void halfSlabIsOneBox() {
        CornerShape slab = CornerShape.cube();
        for (Corner c : Corner.values()) {
            if (c.isMax(Axis.Y)) {
                slab = slab.with(c, Axis.Y, 8);
            }
        }
        VoxelShape shape = FlexiCatShapes.of(slab);
        assertEquals(1, shape.toAabbs().size());
        assertEquals(new AABB(0, 0, 0, 1, 0.5, 1), shape.bounds());
    }

    @Test
    void collisionShapeIsCoarserThanThePickShape() {
        VoxelShape pick = FlexiCatShapes.of(wedge());
        VoxelShape collision = FlexiCatShapes.collisionOf(wedge());
        assertTrue(collision.toAabbs().size() < pick.toAabbs().size(),
                "collision " + collision.toAabbs().size() + " boxes vs pick " + pick.toAabbs().size());
        // Every collision box edge lies on the 4/16 grid.
        for (AABB box : collision.toAabbs()) {
            for (double v : new double[] {box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ}) {
                assertEquals(0.0, (v * 16) % FlexiCatShapes.COLLISION_CELL, 1e-9, "edge at " + v);
            }
        }
        assertSame(Shapes.block(), FlexiCatShapes.collisionOf(CornerShape.cube()));
    }

    @Test
    void rayHitsTheSlopeNotTheBoundingBox() {
        VoxelShape shape = FlexiCatShapes.of(wedge());
        // Centre sampling drops the sliver cells along the sharp edges: the shape ends
        // one grid step short of the top and of the north wall, but stays inside the cell.
        assertEquals(new AABB(0, 0, 1 / 16.0, 1, 15 / 16.0, 1), shape.bounds());
        BlockPos pos = new BlockPos(10, 64, -3);
        // Straight down at z = 0.75 (block units): the ramp surface is at y = 0.75 there.
        Vec3 from = new Vec3(10.5, 66.0, -3 + 0.75);
        Vec3 to = new Vec3(10.5, 63.0, -3 + 0.75);
        BlockHitResult hit = shape.clip(from, to, pos);
        assertNotNull(hit);
        assertEquals(Direction.UP, hit.getDirection());
        double y = hit.getLocation().y - pos.getY();
        assertTrue(y > 0.6 && y < 0.8, "hit at y=" + y + " should follow the slope, not the box top");
        // Above the low (north) end there is nothing to hit.
        assertNull(shape.clip(new Vec3(10.5, 66.0, -3 + 0.1), new Vec3(10.5, 64.5, -3 + 0.1), pos));
    }

    @Test
    void flatPlateFallsBackToPaddedBounds() {
        CornerShape flat = CornerShape.cube();
        for (Corner c : Corner.values()) {
            flat = flat.with(c, Axis.Y, 16);
        }
        VoxelShape shape = FlexiCatShapes.of(flat);
        assertFalse(shape.isEmpty());
        assertEquals(new AABB(0, 15 / 16.0, 0, 1, 1, 1), shape.bounds());
    }

    @Test
    void identicalShapesShareOneVoxelShape() {
        CornerShape a = CornerShape.cube().with(Corner.UP_EAST_SOUTH, Axis.X, 4);
        CornerShape b = CornerShape.cube().with(Corner.UP_EAST_SOUTH, Axis.X, 4);
        assertNotSame(a, b);
        assertSame(FlexiCatShapes.of(a), FlexiCatShapes.of(b));
    }
}