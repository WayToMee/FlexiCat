package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.client.CornerEditClient;
import io.github.waytomee.flexicat.client.FlexiCatKeys;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only entry point: installs the client hooks, registers key mappings and
 * drives the editing session from the tick and world-render events.
 */
@Mod(value = FlexiCat.MOD_ID, dist = Dist.CLIENT)
public final class FlexiCatNeoForgeClient {

    public FlexiCatNeoForgeClient(IEventBus modBus) {
        LoaderHooks.installSendToServer(payload -> PacketDistributor.sendToServer(payload));
        LoaderHooks.installCornerToolUsedOnClient(CornerEditClient::onToolUsed);

        modBus.addListener(FlexiCatNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onRenderLevelStage);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        FlexiCatKeys.all().forEach(event::register);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        CornerEditClient.tick(Minecraft.getInstance());
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        CornerEditClient.render(event.getPoseStack(), event.getCamera().getPosition());
    }
}