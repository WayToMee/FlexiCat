package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.edit.CornerEditServer;
import io.github.waytomee.flexicat.edit.CornerMove;
import io.github.waytomee.flexicat.edit.HandlePicker;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.Vec3i16;
import io.github.waytomee.flexicat.item.CornerToolItem;
import io.github.waytomee.flexicat.network.CornerMovePayload;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Client-side corner editing session.
 *
 * <p>There is at most one session: the block whose handles are shown. Selection is
 * "sticky aim": the handle under the crosshair becomes the selected corner and
 * stays selected until another handle is aimed at, so a corner that has been
 * pushed away from the crosshair keeps receiving moves. Key presses become
 * {@link CornerMovePayload} intents; nothing is predicted locally — the shape
 * changes when the server's block-entity sync arrives.
 *
 * <p>The loader module calls {@link #tick} every client tick and {@link #render}
 * from its world-render hook.
 */
public final class CornerEditClient {

    @Nullable
    private static BlockPos editing;
    @Nullable
    private static Corner selected;

    private CornerEditClient() {
    }

    public static Optional<BlockPos> editing() {
        return Optional.ofNullable(editing);
    }

    public static Optional<Corner> selected() {
        return Optional.ofNullable(selected);
    }

    /** Right-click with the tool on a FlexiCat block: toggle editing for it. */
    public static void onToolUsed(BlockPos pos) {
        if (pos.equals(editing)) {
            stop();
        } else {
            editing = pos.immutable();
            selected = null;
        }
    }

    public static void stop() {
        if (editing != null) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.empty(), false);
        }
        editing = null;
        selected = null;
    }

    public static void tick(Minecraft mc) {
        if (editing == null) {
            drainKeys();
            return;
        }
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null
                || !CornerToolItem.isHeldBy(player)
                || !player.canInteractWithBlock(editing, CornerEditServer.REACH_PADDING)) {
            stop();
            drainKeys();
            return;
        }
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, editing);
        if (target.isEmpty()) {
            stop();
            drainKeys();
            return;
        }
        CornerShape shape = target.get().shape();
        updateSelection(player, shape);
        if (mc.screen == null) {
            handleKeys(player);
        } else {
            drainKeys();
        }
        mc.gui.setOverlayMessage(hud(shape), false);
    }

    /** Draw the handle overlay for the block being edited, if any. */
    public static void render(PoseStack poseStack, Vec3 cameraPos) {
        if (editing == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, editing);
        if (target.isEmpty()) {
            return;
        }
        CornerHandleRenderer.render(poseStack, mc.renderBuffers().bufferSource(), cameraPos,
                editing, target.get().shape(), selected);
    }

    private static void updateSelection(LocalPlayer player, CornerShape shape) {
        BlockPos pos = editing;
        if (pos == null) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double range = player.blockInteractionRange() + CornerEditServer.REACH_PADDING;
        HandlePicker.pick(shape,
                        eye.x - pos.getX(), eye.y - pos.getY(), eye.z - pos.getZ(),
                        look.x, look.y, look.z,
                        HandlePicker.DEFAULT_HALF_SIZE, range)
                .ifPresent(hit -> selected = hit.corner());
    }

    private static void handleKeys(LocalPlayer player) {
        Direction facing = player.getDirection();
        sendMoves(FlexiCatKeys.MOVE_UP, CornerMove.UP);
        sendMoves(FlexiCatKeys.MOVE_DOWN, CornerMove.DOWN);
        sendMoves(FlexiCatKeys.MOVE_LEFT, CornerMove.fromDirection(facing.getCounterClockWise()));
        sendMoves(FlexiCatKeys.MOVE_RIGHT, CornerMove.fromDirection(facing.getClockWise()));
        sendMoves(FlexiCatKeys.MOVE_AWAY, CornerMove.fromDirection(facing));
        sendMoves(FlexiCatKeys.MOVE_TOWARDS, CornerMove.fromDirection(facing.getOpposite()));
    }

    private static void sendMoves(KeyMapping key, CornerMove move) {
        int presses = 0;
        while (key.consumeClick()) {
            presses++;
        }
        BlockPos pos = editing;
        Corner corner = selected;
        if (presses == 0 || pos == null || corner == null) {
            return;
        }
        for (int i = 0; i < presses; i++) {
            LoaderHooks.sendToServer(CornerMovePayload.of(pos, corner, move.axis(), move.delta()));
        }
    }

    /** Consume pending presses so they do not pile up and fire when editing starts. */
    private static void drainKeys() {
        for (KeyMapping key : FlexiCatKeys.all()) {
            while (key.consumeClick()) {
                // discard
            }
        }
    }

    private static Component hud(CornerShape shape) {
        Corner corner = selected;
        if (corner == null) {
            return Component.translatable("message.flexicat.hud.aim");
        }
        Vec3i16 p = shape.position(corner);
        return Component.translatable("message.flexicat.hud.selected",
                Component.translatable("corner.flexicat." + corner.name().toLowerCase(Locale.ROOT)),
                p.x(), p.y(), p.z(),
                FlexiCatKeys.MOVE_UP.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_DOWN.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_LEFT.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_RIGHT.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_AWAY.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_TOWARDS.getTranslatedKeyMessage());
    }
}