package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.waytomee.flexicat.edit.HandlePicker;
import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.Vec3i16;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the editing overlay for one block: the twelve edges of the current shape
 * and a small box at each of the eight corners, with the selected corner in a
 * highlight colour. Everything is derived from {@link CornerShape}; the
 * placeholder block model is not consulted.
 *
 * <p>{@link #renderOutline} draws only the twelve edges in vanilla's hit-outline
 * style. The loader's client module uses it to replace the default block outline,
 * which would otherwise show the stepped boxes of the voxelised collision shape.
 */
public final class CornerHandleRenderer {

    private static final int EDGE_COLOR = 0xD0FFFFFF;
    private static final int HANDLE_COLOR = 0xFF46B8FF;
    private static final int SELECTED_COLOR = 0xFFFFC53D;

    private CornerHandleRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera,
                              BlockPos pos, CornerShape shape, @Nullable Corner selected) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer lines = buffers.getBuffer(FlexiCatRenderTypes.OVERLAY_LINES);

        edges(pose, lines, shape, EDGE_COLOR);

        // Handles: boxes centred on the corner positions.
        double h = HandlePicker.DEFAULT_HALF_SIZE;
        for (Corner corner : Corner.values()) {
            Vec3i16 p = shape.position(corner);
            double x = p.xBlocks();
            double y = p.yBlocks();
            double z = p.zBlocks();
            int color = corner == selected ? SELECTED_COLOR : HANDLE_COLOR;
            LevelRenderer.renderLineBox(poseStack, lines,
                    x - h, y - h, z - h, x + h, y + h, z + h,
                    red(color), green(color), blue(color), alpha(color));
        }

        buffers.endBatch(FlexiCatRenderTypes.OVERLAY_LINES);
        poseStack.popPose();
    }

    /**
     * The true edges of the shape as a block-selection outline (vanilla draws
     * black at 40% alpha). {@code consumer} should be the {@code RenderType.lines()}
     * buffer the game uses for its own outline.
     */
    public static void renderOutline(PoseStack poseStack, VertexConsumer consumer, Vec3 camera,
                                     BlockPos pos, CornerShape shape, int color) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        edges(poseStack.last(), consumer, shape, color);
        poseStack.popPose();
    }

    /** Each corner to its neighbour across every axis, each edge once. */
    private static void edges(PoseStack.Pose pose, VertexConsumer consumer, CornerShape shape, int color) {
        for (Corner corner : Corner.values()) {
            for (Axis axis : Axis.values()) {
                if (!corner.isMax(axis)) {
                    line(pose, consumer, shape.position(corner), shape.position(corner.across(axis)), color);
                }
            }
        }
    }

    private static void line(PoseStack.Pose pose, VertexConsumer consumer, Vec3i16 from, Vec3i16 to, int color) {
        float x1 = (float) from.xBlocks();
        float y1 = (float) from.yBlocks();
        float z1 = (float) from.zBlocks();
        float x2 = (float) to.xBlocks();
        float y2 = (float) to.yBlocks();
        float z2 = (float) to.zBlocks();
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0e-6F) {
            return; // two corners at the same point: no edge to draw
        }
        nx /= len;
        ny /= len;
        nz /= len;
        consumer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(pose, nx, ny, nz);
    }

    private static float red(int argb) {
        return ((argb >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int argb) {
        return ((argb >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int argb) {
        return (argb & 0xFF) / 255.0F;
    }

    private static float alpha(int argb) {
        return ((argb >>> 24) & 0xFF) / 255.0F;
    }
}