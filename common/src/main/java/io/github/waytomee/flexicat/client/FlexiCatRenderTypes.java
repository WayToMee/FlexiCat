package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * Render types for the editing overlay.
 *
 * <p>{@link #OVERLAY_LINES} is the vanilla line type with the depth test turned
 * off. While the block still renders as a placeholder full cube (stage 4 brings
 * the real mesh), moved corners sit inside that cube and would be hidden by a
 * depth-tested line; without the test the handles stay visible. The cost is that
 * the overlay also shows through walls — acceptable for an editing aid, and to
 * be revisited once the block renders its true shape.
 */
public final class FlexiCatRenderTypes extends RenderType {

    public static final RenderType OVERLAY_LINES = create("flexicat_overlay_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));

    // Never instantiated; subclassing only gives access to the protected state shards.
    private FlexiCatRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}