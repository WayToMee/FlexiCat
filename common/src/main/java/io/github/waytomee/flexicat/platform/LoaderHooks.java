package io.github.waytomee.flexicat.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The few places where common code needs the loader.
 *
 * <p>Everything in {@code common} compiles against plain Minecraft, so sending a
 * network payload (loader-specific registration) and reacting to the tool on the
 * client (client-only classes must not be touched from shared item code on a
 * dedicated server) go through these hooks. Each loader module installs its
 * implementations during startup. The defaults fail loudly rather than silently
 * so a missing installation shows up in a dev run immediately.
 */
public final class LoaderHooks {

    private static Consumer<CustomPacketPayload> sendToServer = payload -> {
        throw new IllegalStateException("FlexiCat: sendToServer hook not installed by the loader module");
    };
    private static Consumer<BlockPos> cornerToolUsedOnClient = pos -> {
        // No-op on a dedicated server, where nothing installs the client hook.
    };

    private LoaderHooks() {
    }

    /** Loader module (client side): how a payload reaches the server. */
    public static void installSendToServer(Consumer<CustomPacketPayload> sender) {
        sendToServer = Objects.requireNonNull(sender, "sender");
    }

    /** Loader module (client side): what happens when the corner tool is used on a FlexiCat block. */
    public static void installCornerToolUsedOnClient(Consumer<BlockPos> handler) {
        cornerToolUsedOnClient = Objects.requireNonNull(handler, "handler");
    }

    public static void sendToServer(CustomPacketPayload payload) {
        sendToServer.accept(payload);
    }

    public static void cornerToolUsedOnClient(BlockPos pos) {
        cornerToolUsedOnClient.accept(pos);
    }
}