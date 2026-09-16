package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.FlexiCat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FlexiCat.MOD_ID)
public final class FlexiCatNeoForge {
    public FlexiCatNeoForge(IEventBus modBus) {
        FlexiCat.LOGGER.info("{} is loading on NeoForge", FlexiCat.MOD_NAME);
    }
}