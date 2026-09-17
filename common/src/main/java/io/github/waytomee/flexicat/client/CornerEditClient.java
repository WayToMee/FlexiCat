package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.edit.CornerEditServer;
import io.github.waytomee.flexicat.edit.CornerMove;
import io.github.waytomee.flexicat.edit.HandlePicker;
import io.github.waytomee.flexicat.edit.HeldKeyRepeater;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Client-side corner editing session.
 *
 * <p>There is at most one session: the block whose handles are shown. The handle
 * under the crosshair is only <em>hovered</em> (highlighted); it becomes the
 * <em>selected</em> corner when the player right-clicks it with the tool, and stays
 * selected until another handle is clicked, so the aim is free to wander while keys
 * move the corner. Key presses become {@link CornerMovePayload} intents; nothing is
 * predicted locally — the shape changes when the server's block-entity sync arrives.
 *
 * <p>Move keys are sampled as held/released each tick and turned into repeats by
 * {@link HeldKeyRepeater}, so holding an arrow key keeps moving the corner even while
 * walking (the OS only repeats the last key pressed), and the repeat rate caps how
 * often the shape is rebuilt.
 *
 * <p>The loader module calls {@link #tick} every client tick, {@link #onUseKey} when
 * the use key is pressed (before vanilla handles it) and {@link #render} from its
 * world-render hook.
 */
public final class CornerEditClient {

    private static final HeldKeyRepeater REPEATER = new HeldKeyRepeater(FlexiCatKeys.all().size(),
            HeldKeyRepeater.DEFAULT_INITIAL_DELAY, HeldKeyRepeater.DEFAULT_REPEAT_INTERVAL);

    @Nullable
    private static BlockPos editing;
    @Nullable
    private static Corner selected;
    @Nullable
    private static Corner hovered;

    private CornerEditClient() {
    }

    public static Optional<BlockPos> editing() {
        return Optional.ofNullable(editing);
    }

    public static Optional<Corner> selected() {
        return Optional.ofNullable(selected);
    }

    /** Right-click with the tool on a FlexiCat block (not on a handle): toggle editing for it. */
    public static void onToolUsed(BlockPos pos) {
        if (pos.equals(editing)) {
            stop();
        } else {
            editing = pos.immutable();
            selected = null;
            hovered = null;
            REPEATER.reset();
        }
    }

    public static void stop() {
        if (editing != null) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.empty(), false);
        }
        editing = null;
        selected = null;
        hovered = null;
        REPEATER.reset();
    }

    /**
     * The use key was pressed. If a handle of the edited block is under the crosshair
     * (tool in hand, not sneaking), select it and consume the click so vanilla does not
     * also interact with the block behind it.
     *
     * @return {@code true} if the click selected a handle and must not reach vanilla
     */
    public static boolean onUseKey(Minecraft mc) {
        BlockPos pos = editing;
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (pos == null || player == null || level == null
                || !CornerToolItem.isHeldBy(player) || player.isSecondaryUseActive()) {
            return false;
        }
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, pos);
        if (target.isEmpty()) {
            return false;
        }
        Optional<Corner> hit = pickHandle(player, pos, target.get().shape());
        if (hit.isEmpty()) {
            return false;
        }
        selected = hit.get();
        return true;
    }

    public static void tick(Minecraft mc) {
        drainClicks();
        if (editing == null) {
            return;
        }
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null
                || !CornerToolItem.isHeldBy(player)
                || !player.canInteractWithBlock(editing, CornerEditServer.REACH_PADDING)) {
            stop();
            return;
        }
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, editing);
        if (target.isEmpty()) {
            stop();
            return;
        }
        CornerShape shape = target.get().shape();
        hovered = pickHandle(player, editing, shape).orElse(null);
        if (mc.screen == null) {
            handleKeys(player, shape);
        } else {
            REPEATER.reset();
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
                editing, target.get().shape(), selected, hovered);
    }

    private static Optional<Corner> pickHandle(LocalPlayer player, BlockPos pos, CornerShape shape) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double range = player.blockInteractionRange() + CornerEditServer.REACH_PADDING;
        return HandlePicker.pick(shape,
                        eye.x - pos.getX(), eye.y - pos.getY(), eye.z - pos.getZ(),
                        look.x, look.y, look.z,
                        HandlePicker.DEFAULT_HALF_SIZE, range)
                .map(HandlePicker.Hit::corner);
    }

    private static void handleKeys(LocalPlayer player, CornerShape shape) {
        Direction facing = player.getDirection();
        List<KeyMapping> keys = FlexiCatKeys.all();
        for (int i = 0; i < keys.size(); i++) {
            KeyMapping key = keys.get(i);
            if (REPEATER.tick(i, key.isDown())) {
                send(shape, moveFor(key, facing));
            }
        }
    }

    private static CornerMove moveFor(KeyMapping key, Direction facing) {
        if (key == FlexiCatKeys.MOVE_UP) {
            return CornerMove.UP;
        }
        if (key == FlexiCatKeys.MOVE_DOWN) {
            return CornerMove.DOWN;
        }
        if (key == FlexiCatKeys.MOVE_LEFT) {
            return CornerMove.fromDirection(facing.getCounterClockWise());
        }
        if (key == FlexiCatKeys.MOVE_RIGHT) {
            return CornerMove.fromDirection(facing.getClockWise());
        }
        if (key == FlexiCatKeys.MOVE_AWAY) {
            return CornerMove.fromDirection(facing);
        }
        return CornerMove.fromDirection(facing.getOpposite());
    }

    /** Send one move intent, unless it could not change the shape (corner already clamped). */
    private static void send(CornerShape shape, CornerMove move) {
        BlockPos pos = editing;
        Corner corner = selected;
        if (pos == null || corner == null) {
            return;
        }
        if (shape.move(corner, move.axis(), move.delta()).equals(shape)) {
            return; // at the edge of the cell on that axis: nothing to do
        }
        LoaderHooks.sendToServer(CornerMovePayload.of(pos, corner, move.axis(), move.delta()));
    }

    /** Presses are read as held state, so vanilla's click counter is never used; keep it empty. */
    private static void drainClicks() {
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