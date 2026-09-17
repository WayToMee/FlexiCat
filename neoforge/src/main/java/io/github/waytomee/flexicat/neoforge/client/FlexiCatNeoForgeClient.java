package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.client.CornerEditClient;
import io.github.waytomee.flexicat.client.CornerHandleRenderer;
import io.github.waytomee.flexicat.client.FlexiCatKeys;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import io.github.waytomee.flexicat.neoforge.FlexiCatRegistration;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only entry point: installs the client hooks, registers key mappings,
 * drives the editing session from the tick and world-render events and replaces
 * the block outline of FlexiCat blocks with the shape's true edges.
 */
@Mod(value = FlexiCat.MOD_ID, dist = Dist.CLIENT)
public final class FlexiCatNeoForgeClient {

    public FlexiCatNeoForgeClient(IEventBus modBus, ModContainer container) {
        LoaderHooks.installSendToServer(payload -> PacketDistributor.sendToServer(payload));
        LoaderHooks.installCornerToolUsedOnClient(CornerEditClient::onToolUsed);
        FlexiCatNeoForgeClientConfig.register(container);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(FlexiCatNeoForgeClient::onRegisterKeyMappings);
        modBus.addListener(FlexiCatNeoForgeClient::onModifyBakingResult);
        modBus.addListener(FlexiCatNeoForgeClient::onRegisterBlockColors);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(FlexiCatNeoForgeClient::onRenderBlockHighlight);
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

    /**
     * Tinted materials (grass, leaves, ...) keep their biome colour: the FlexiCat block
     * forwards colour queries to the material's own colour handler.
     */
    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return -1;
            }
            return FlexiCatBlock.entityAt(level, pos).flatMap(FlexiCatBlockEntity::material)
                    .map(material -> Minecraft.getInstance().getBlockColors().getColor(material, level, pos, tintIndex))
                    .orElse(-1);
        }, FlexiCatRegistration.FLEXICAT_BLOCK.get());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        CornerEditClient.tick(Minecraft.getInstance());
    }

    /**
     * Right-click on a corner handle selects it. The event fires before vanilla's use
     * handling, once per hand; cancelling it for the main hand stops the whole use.
     */
    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (CornerEditClient.onUseKey(Minecraft.getInstance())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        CornerEditClient.render(event.getPoseStack(), event.getCamera().getPosition());
    }

    /** Vanilla's outline colour: black at 40% alpha. */
    private static final int OUTLINE_COLOR = 0x66000000;

    /**
     * Vanilla outlines the voxel shape, which for a deformed block is a staircase of
     * small boxes. Draw the twelve real edges instead; the voxel shape still decides
     * <em>what</em> is targeted, this only changes what the player sees.
     */
    private static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        FlexiCatBlock.entityAt(mc.level, pos).ifPresent(be -> {
            if (be.shape().isCube()) {
                return; // the default outline is already exact
            }
            CornerHandleRenderer.renderOutline(event.getPoseStack(),
                    event.getMultiBufferSource().getBuffer(RenderType.lines()),
                    event.getCamera().getPosition(), pos, be.shape(), OUTLINE_COLOR);
            event.setCanceled(true);
        });
    }
}
