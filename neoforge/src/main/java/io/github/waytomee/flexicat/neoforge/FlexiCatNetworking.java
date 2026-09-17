package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.edit.CornerEditServer;
import io.github.waytomee.flexicat.network.CornerMovePayload;
import io.github.waytomee.flexicat.network.ToolActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge payload registration. The payload types and the server-side handling
 * live in {@code common}; only the wiring is loader-specific.
 */
public final class FlexiCatNetworking {

    /** Bump when a payload's wire format changes incompatibly. 2: corner index → corner mask (stage 7). */
    public static final String PROTOCOL_VERSION = "2";

    private FlexiCatNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FlexiCatNetworking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CornerMovePayload.TYPE, CornerMovePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                context.enqueueWork(() -> CornerEditServer.handleMove(serverPlayer, payload));
            }
        });
        registrar.playToServer(ToolActionPayload.TYPE, ToolActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                context.enqueueWork(() -> CornerEditServer.handleToolAction(serverPlayer, payload));
            }
        });
    }
}