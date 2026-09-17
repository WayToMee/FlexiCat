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
 * Converts a {@link CornerShape} into the {@link VoxelShape} Minecraft uses for
 * collision, block picking (ray casting) and support checks.
 *
 * <p>The geometry comes from {@link ShapeVoxelizer}: boxes on the 1/16 grid whose
 * centres lie inside the deformed hull. A shape without volume (a flat plate) falls
 * back to its bounding box padded to 1/16 so it can still be targeted and stood on.
 *
 * <p>Shapes are immutable and many placed blocks share the same one, so results are
 * cached process-wide (bounded LRU) in addition to the per-block-entity cache.
 */
public final class FlexiCatShapes {

    private static final int CACHE_SIZE = 512;
    private static final Map<CornerShape, VoxelShape> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CornerShape, VoxelShape> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private FlexiCatShapes() {
    }

    /** Collision / outline shape for {@code shape}, in block units. */
    public static VoxelShape of(CornerShape shape) {
        if (shape.isCube()) {
            return Shapes.block();
        }
        synchronized (CACHE) {
            VoxelShape cached = CACHE.get(shape);
            if (cached != null) {
                return cached;
            }
        }
        VoxelShape built = build(shape);
        synchronized (CACHE) {
            CACHE.putIfAbsent(shape, built);
            return CACHE.get(shape);
        }
    }

    static VoxelShape build(CornerShape shape) {
        List<ShapeVoxelizer.Box> boxes = ShapeVoxelizer.voxelize(shape);
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