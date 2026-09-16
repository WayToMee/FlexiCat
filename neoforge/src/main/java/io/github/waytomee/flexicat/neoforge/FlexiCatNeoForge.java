package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.FlexiCat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Common (both sides) entry point. The client-only counterpart is
 * {@code io.github.waytomee.flexicat.neoforge.client.FlexiCatNeoForgeClient}.
 */
@Mod(FlexiCat.MOD_ID)
public final class FlexiCatNeoForge {
    public FlexiCatNeoForge(IEventBus modBus) {
        FlexiCat.LOGGER.info("{} is loading on NeoForge", FlexiCat.MOD_NAME);
        FlexiCatRegistration.register(modBus);
        FlexiCatNetworking.register(modBus);
    }
}