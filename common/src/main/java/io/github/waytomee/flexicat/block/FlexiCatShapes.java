package io.github.waytomee.flexicat.block;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.ShapeVoxelizer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a {@link CornerShape} into the {@link VoxelShape}s Minecraft uses for
 * block picking (ray casting), the outline, and entity collision.
 *
 * <p>Two shapes are produced from the same geometry ({@link ShapeVoxelizer}):
 * <ul>
 *   <li>{@link #of}: cells of 1/16 — the tight staircase used for picking, so the
 *       crosshair lands within one grid step of the real surface.</li>
 *   <li>{@link #collisionOf}: cells of {@value #COLLISION_CELL}/16 — the same hull
 *       with fewer, taller steps. Vanilla's step-up logic climbs every box edge
 *       separately, so a 1/16 staircase makes players crawl and bob; 4/16 steps are
 *       climbed at walking speed (like stairs) at the cost of the surface being off
 *       by up to 2/16 either way.</li>
 * </ul>
 * A shape without volume (a flat plate) falls back to its bounding box padded to
 * 1/16 so it can still be targeted and stood on.
 *
 * <p>Shapes are immutable and many placed blocks share the same one, so results are
 * cached process-wide (bounded LRU) in addition to the per-block-entity cache.
 */
public final class FlexiCatShapes {

    /** Cell size, in grid units, of the collision approximation. */
    public static final int COLLISION_CELL = 4;

    private static final int CACHE_SIZE = 512;
    private static final Map<CornerShape, VoxelShape> PICK_CACHE = lru();
    private static final Map<CornerShape, VoxelShape> COLLISION_CACHE = lru();

    private static Map<CornerShape, VoxelShape> lru() {
        return new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CornerShape, VoxelShape> eldest) {
                return size() > CACHE_SIZE;
            }
        };
    }

    private FlexiCatShapes() {
    }

    /** Picking / outline shape for {@code shape} (1/16 cells), in block units. */
    public static VoxelShape of(CornerShape shape) {
        return cached(PICK_CACHE, shape, 1);
    }

    /** Entity collision shape for {@code shape} ({@value #COLLISION_CELL}/16 cells), in block units. */
    public static VoxelShape collisionOf(CornerShape shape) {
        return cached(COLLISION_CACHE, shape, COLLISION_CELL);
    }

    private static VoxelShape cached(Map<CornerShape, VoxelShape> cache, CornerShape shape, int cell) {
        if (shape.isCube()) {
            return Shapes.block();
        }
        synchronized (cache) {
            VoxelShape hit = cache.get(shape);
            if (hit != null) {
                return hit;
            }
        }
        VoxelShape built = build(shape, cell);
        synchronized (cache) {
            cache.putIfAbsent(shape, built);
            return cache.get(shape);
        }
    }

    static VoxelShape build(CornerShape shape) {
        return build(shape, 1);
    }

    static VoxelShape build(CornerShape shape, int cell) {
        List<ShapeVoxelizer.Box> boxes = ShapeVoxelizer.voxelize(shape, cell);
        if (boxes.isEmpty()) {
            return paddedBounds(shape);
        }
        double g = CornerShape.GRID;
        VoxelShape result = null;
        for (ShapeVoxelizer.Box b : boxes) {
            VoxelShape box = Shapes.box(b.minX() / g, b.minY() / g, b.minZ() / g, b.maxX() / g, b.maxY() / g, b.maxZ() / g);
            result = result == null ? box : Shapes.or(result, box);
        }
        return result.optimize();
    }

    /**
     * Axis-aligned box around all eight corners; a zero-thickness axis is grown by
     * one grid step towards the inside of the cell. Used for shapes with no volume.
     */
    public static VoxelShape paddedBounds(CornerShape shape) {
        int[] b = shape.bounds();
        for (Axis axis : Axis.values()) {
            int i = axis.ordinal() * 2;
            if (b[i + 1] - b[i] < 1) {
                if (b[i] > CornerShape.MIN) {
                    b[i]--;
                } else {
                    b[i + 1]++;
                }
            }
        }
        double g = CornerShape.GRID;
        return Shapes.box(b[0] / g, b[2] / g, b[4] / g, b[1] / g, b[3] / g, b[5] / g);
    }
}