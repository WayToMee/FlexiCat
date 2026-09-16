package io.github.waytomee.flexicat;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic constants shared by every module.
 */
public final class FlexiCat {
    public static final String MOD_ID = "flexicat";
    public static final String MOD_NAME = "FlexiCat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private FlexiCat() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}