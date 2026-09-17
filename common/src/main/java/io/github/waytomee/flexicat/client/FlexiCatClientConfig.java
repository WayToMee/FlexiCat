package io.github.waytomee.flexicat.client;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

/**
 * Client-side tuning knobs (stage 7). Common code reads them through suppliers; the
 * loader module binds those to its own config system (NeoForge: {@code ModConfigSpec},
 * file {@code config/flexicat-client.toml}). Until a loader binds them the defaults
 * apply, so a dedicated server or a unit test never sees a missing config.
 */
public final class FlexiCatClientConfig {

    public static final int DEFAULT_REPEAT_DELAY_TICKS = 6;
    public static final int DEFAULT_REPEAT_INTERVAL_TICKS = 2;
    public static final double DEFAULT_HANDLE_SIZE = 1.0;
    public static final boolean DEFAULT_MOVE_SOUNDS = true;
    public static final boolean DEFAULT_SHOW_HUD = true;

    private static IntSupplier repeatDelay = () -> DEFAULT_REPEAT_DELAY_TICKS;
    private static IntSupplier repeatInterval = () -> DEFAULT_REPEAT_INTERVAL_TICKS;
    private static DoubleSupplier handleSize = () -> DEFAULT_HANDLE_SIZE;
    private static BooleanSupplier moveSounds = () -> DEFAULT_MOVE_SOUNDS;
    private static BooleanSupplier showHud = () -> DEFAULT_SHOW_HUD;

    private FlexiCatClientConfig() {
    }

    /** Loader module: plug in the live config values. Any argument may be {@code null} to keep the default. */
    public static void bind(IntSupplier repeatDelayTicks, IntSupplier repeatIntervalTicks,
                            DoubleSupplier handleSizeGrid, BooleanSupplier moveSoundsEnabled, BooleanSupplier hudEnabled) {
        if (repeatDelayTicks != null) repeatDelay = repeatDelayTicks;
        if (repeatIntervalTicks != null) repeatInterval = repeatIntervalTicks;
        if (handleSizeGrid != null) handleSize = handleSizeGrid;
        if (moveSoundsEnabled != null) moveSounds = moveSoundsEnabled;
        if (hudEnabled != null) showHud = hudEnabled;
    }

    /** Ticks a move key must be held before it starts repeating. */
    public static int repeatDelayTicks() {
        return Math.max(1, repeatDelay.getAsInt());
    }

    /** Ticks between repeats while a move key stays held. */
    public static int repeatIntervalTicks() {
        return Math.max(1, repeatInterval.getAsInt());
    }

    /** Handle half-size in grid units (1/16 block). */
    public static double handleHalfSizeBlocks() {
        return Math.max(0.5, handleSize.getAsDouble()) / 16.0;
    }

    /** Whether each corner move plays a quiet click on the client. */
    public static boolean moveSounds() {
        return moveSounds.getAsBoolean();
    }

    /** Whether the editing HUD line above the hotbar is shown. */
    public static boolean showHud() {
        return showHud.getAsBoolean();
    }
}