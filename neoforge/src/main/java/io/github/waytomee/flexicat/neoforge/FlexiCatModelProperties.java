package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.geometry.CornerShape;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Model-data keys the NeoForge block entity publishes for the dynamic block model. */
public final class FlexiCatModelProperties {

    /** The block's current {@link CornerShape}. */
    public static final ModelProperty<CornerShape> SHAPE = new ModelProperty<>();

    private FlexiCatModelProperties() {
    }
}