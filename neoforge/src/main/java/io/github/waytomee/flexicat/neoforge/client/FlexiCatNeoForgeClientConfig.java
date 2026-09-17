package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.client.FlexiCatClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge binding of the client config ({@code config/flexicat-client.toml}). The
 * values are read live, so edits via a config screen or a file reload take effect
 * without restarting.
 */
public final class FlexiCatNeoForgeClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.IntValue REPEAT_DELAY = BUILDER
            .comment("Ticks a move key must be held before the corner starts repeating (20 ticks = 1 s).")
            .defineInRange("editing.repeatDelayTicks", FlexiCatClientConfig.DEFAULT_REPEAT_DELAY_TICKS, 1, 40);
    static final ModConfigSpec.IntValue REPEAT_INTERVAL = BUILDER
            .comment("Ticks between corner moves while a move key stays held.")
            .defineInRange("editing.repeatIntervalTicks", FlexiCatClientConfig.DEFAULT_REPEAT_INTERVAL_TICKS, 1, 20);
    static final ModConfigSpec.DoubleValue HANDLE_SIZE = BUILDER
            .comment("Half-size of a corner handle in 1/16 block units (bigger = easier to click).")
            .defineInRange("editing.handleSize", FlexiCatClientConfig.DEFAULT_HANDLE_SIZE, 0.5, 4.0);
    static final ModConfigSpec.BooleanValue MOVE_SOUNDS = BUILDER
            .comment("Play a quiet click for every corner move and handle selection.")
            .define("editing.moveSounds", FlexiCatClientConfig.DEFAULT_MOVE_SOUNDS);
    static final ModConfigSpec.BooleanValue SHOW_HUD = BUILDER
            .comment("Show the editing hint line above the hotbar while a block is being edited.")
            .define("editing.showHud", FlexiCatClientConfig.DEFAULT_SHOW_HUD);

    static final ModConfigSpec SPEC = BUILDER.build();

    private FlexiCatNeoForgeClientConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
        FlexiCatClientConfig.bind(
                () -> SPEC.isLoaded() ? REPEAT_DELAY.getAsInt() : FlexiCatClientConfig.DEFAULT_REPEAT_DELAY_TICKS,
                () -> SPEC.isLoaded() ? REPEAT_INTERVAL.getAsInt() : FlexiCatClientConfig.DEFAULT_REPEAT_INTERVAL_TICKS,
                () -> SPEC.isLoaded() ? HANDLE_SIZE.getAsDouble() : FlexiCatClientConfig.DEFAULT_HANDLE_SIZE,
                () -> SPEC.isLoaded() ? MOVE_SOUNDS.getAsBoolean() : FlexiCatClientConfig.DEFAULT_MOVE_SOUNDS,
                () -> SPEC.isLoaded() ? SHOW_HUD.getAsBoolean() : FlexiCatClientConfig.DEFAULT_SHOW_HUD);
    }
}