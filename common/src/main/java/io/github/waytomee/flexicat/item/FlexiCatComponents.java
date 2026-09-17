package io.github.waytomee.flexicat.item;

import io.github.waytomee.flexicat.codec.CornerShapeCodecs;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.component.DataComponentType;

/**
 * Item data components. The objects are created here so common code can read and
 * write them; each loader module registers them under the fixed registry names.
 */
public final class FlexiCatComponents {

    /** Registry name of {@link #SHAPE}: {@code flexicat:shape}. Part of the item format. */
    public static final String SHAPE_NAME = "shape";

    /**
     * The shape a corner tool has copied (stage 7). Persisted on the item (24-byte
     * form via {@link CornerShapeCodecs#CODEC}) and synced so the client can show it
     * in the tooltip. Absent until the tool has copied something.
     */
    public static final DataComponentType<CornerShape> SHAPE = DataComponentType.<CornerShape>builder()
            .persistent(CornerShapeCodecs.CODEC)
            .networkSynchronized(CornerShapeCodecs.STREAM_CODEC)
            .build();

    private FlexiCatComponents() {
    }
}