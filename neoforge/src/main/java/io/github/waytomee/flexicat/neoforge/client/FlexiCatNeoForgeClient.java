package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.client.CornerEditClient;
import io.github.waytomee.flexicat.client.FlexiCatKeys;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import io.github.waytomee.flexicat.neoforge.FlexiCatRegistration;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
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
        modBus.addListener(FlexiCatNeoForgeClient::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onRenderLevelStage);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        FlexiCatKeys.all().forEach(event::register);
    }

    /**
     * Swap the JSON placeholder model for the dynamic one. The placeholder stays
     * wrapped inside: it provides texture, particle sprite and item rendering.
     */
    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (BlockState state : FlexiCatRegistration.FLEXICAT_BLOCK.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel original = event.getModels().get(location);
            if (original != null) {
                event.getModels().put(location, new FlexiCatBakedModel(original));
            } else {
                FlexiCat.LOGGER.warn("No baked model found for {}; FlexiCat block will keep its placeholder", location);
            }
        }
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