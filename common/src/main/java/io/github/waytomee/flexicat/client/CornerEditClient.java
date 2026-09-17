package io.github.waytomee.flexicat.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.edit.CornerEditServer;
import io.github.waytomee.flexicat.edit.CornerMove;
import io.github.waytomee.flexicat.edit.CornerSelection;
import io.github.waytomee.flexicat.edit.HandlePicker;
import io.github.waytomee.flexicat.edit.HeldKeyRepeater;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.Vec3i16;
import io.github.waytomee.flexicat.item.CornerToolItem;
import io.github.waytomee.flexicat.item.FlexiCatComponents;
import io.github.waytomee.flexicat.network.CornerMovePayload;
import io.github.waytomee.flexicat.network.ToolActionPayload;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Client-side corner editing session.
 *
 * <p>There is at most one session: the block whose handles are shown. The handle
 * under the crosshair is only <em>hovered</em> (highlighted); a right-click with the
 * tool makes it the <em>selection</em>, and Ctrl (or Shift) + right-click toggles it
 * in or out of a <em>group</em>, so several corners — a whole face, say — move with one
 * key. Key presses become {@link CornerMovePayload} intents carrying the selection
 * mask; nothing is predicted locally — the shape changes when the server's
 * block-entity sync arrives.
 *
 * <p>Move keys are sampled as held/released each tick and turned into repeats by
 * {@link HeldKeyRepeater}, so holding an arrow key keeps moving the corner even while
 * walking (the OS only repeats the last key pressed), and the repeat rate caps how
 * often the shape is rebuilt.
 *
 * <p>Copy/paste keys work on the block being edited or, when not editing, on the
 * FlexiCat block under the crosshair; they send {@link ToolActionPayload} intents.
 *
 * <p>The loader module calls {@link #tick} every client tick, {@link #onUseKey} when
 * the use key is pressed (before vanilla handles it) and {@link #render} from its
 * world-render hook.
 */
public final class CornerEditClient {

    private static HeldKeyRepeater repeater = newRepeater();
    private static int repeaterDelay;
    private static int repeaterInterval;

    private static final CornerSelection SELECTION = new CornerSelection();
    @Nullable
    private static BlockPos editing;
    @Nullable
    private static Corner hovered;
    private static boolean copyWasDown;
    private static boolean pasteWasDown;

    private CornerEditClient() {
    }

    public static Optional<BlockPos> editing() {
        return Optional.ofNullable(editing);
    }

    /** The corners that currently receive moves (read-only view). */
    public static CornerSelection selection() {
        return SELECTION;
    }

    /** Right-click with the tool on a FlexiCat block (not on a handle): toggle editing for it. */
    public static void onToolUsed(BlockPos pos) {
        if (pos.equals(editing)) {
            stop();
        } else {
            editing = pos.immutable();
            SELECTION.clear();
            hovered = null;
            repeater.reset();
        }
    }

    public static void stop() {
        if (editing != null) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.empty(), false);
        }
        editing = null;
        SELECTION.clear();
        hovered = null;
        repeater.reset();
    }

    /**
     * The use key was pressed. If a handle of the edited block is under the crosshair
     * (tool in hand, not sneaking), select it — or, with Ctrl/Shift held, toggle it in
     * the group — and consume the click so vanilla does not also interact with the block
     * behind it.
     *
     * @return {@code true} if the click hit a handle and must not reach vanilla
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
        if (isGroupModifierDown(mc)) {
            SELECTION.toggle(hit.get());
        } else {
            SELECTION.select(hit.get());
        }
        playTick(mc, 1.6F);
        return true;
    }

    public static void tick(Minecraft mc) {
        drainClicks();
        refreshRepeater();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            stop();
            return;
        }
        boolean toolHeld = CornerToolItem.isHeldBy(player);
        if (mc.screen == null && toolHeld) {
            handleClipboardKeys(mc, player, level);
        } else {
            copyWasDown = false;
            pasteWasDown = false;
        }
        if (editing == null) {
            return;
        }
        if (!toolHeld || !player.canInteractWithBlock(editing, CornerEditServer.REACH_PADDING)) {
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
            handleMoveKeys(mc, player, shape);
        } else {
            repeater.reset();
        }
        if (FlexiCatClientConfig.showHud()) {
            mc.gui.setOverlayMessage(hud(shape), false);
        }
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
                editing, target.get().shape(), SELECTION.mask(), hovered);
    }

    private static Optional<Corner> pickHandle(LocalPlayer player, BlockPos pos, CornerShape shape) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double range = player.blockInteractionRange() + CornerEditServer.REACH_PADDING;
        return HandlePicker.pick(shape,
                        eye.x - pos.getX(), eye.y - pos.getY(), eye.z - pos.getZ(),
                        look.x, look.y, look.z,
                        FlexiCatClientConfig.handleHalfSizeBlocks(), range)
                .map(HandlePicker.Hit::corner);
    }

    private static void handleMoveKeys(Minecraft mc, LocalPlayer player, CornerShape shape) {
        Direction facing = player.getDirection();
        List<KeyMapping> keys = FlexiCatKeys.moves();
        for (int i = 0; i < keys.size(); i++) {
            KeyMapping key = keys.get(i);
            if (repeater.tick(i, key.isDown())) {
                send(mc, shape, moveFor(key, facing));
            }
        }
    }

    /** Copy/paste fire once per press (edge-triggered); the target is the edited block or the aimed one. */
    private static void handleClipboardKeys(Minecraft mc, LocalPlayer player, ClientLevel level) {
        boolean copy = FlexiCatKeys.COPY_SHAPE.isDown();
        boolean paste = FlexiCatKeys.PASTE_SHAPE.isDown();
        if (copy && !copyWasDown) {
            clipboardTarget(mc, level).ifPresent(pos -> LoaderHooks.sendToServer(
                    ToolActionPayload.of(pos, ToolActionPayload.Action.COPY)));
        }
        if (paste && !pasteWasDown) {
            if (CornerToolItem.heldBy(player).get(FlexiCatComponents.SHAPE) == null) {
                mc.gui.setOverlayMessage(Component.translatable("message.flexicat.tool_empty"), false);
            } else {
                clipboardTarget(mc, level).ifPresent(pos -> LoaderHooks.sendToServer(
                        ToolActionPayload.of(pos, ToolActionPayload.Action.PASTE)));
            }
        }
        copyWasDown = copy;
        pasteWasDown = paste;
    }

    private static Optional<BlockPos> clipboardTarget(Minecraft mc, ClientLevel level) {
        if (editing != null) {
            return Optional.of(editing);
        }
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && FlexiCatBlock.entityAt(level, hit.getBlockPos()).isPresent()) {
            return Optional.of(hit.getBlockPos());
        }
        return Optional.empty();
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

    /** Send one move intent for the selection, unless it could not change the shape (group already clamped). */
    private static void send(Minecraft mc, CornerShape shape, CornerMove move) {
        BlockPos pos = editing;
        if (pos == null || SELECTION.isEmpty()) {
            return;
        }
        if (shape.moveGroup(SELECTION.corners(), move.axis(), move.delta()).equals(shape)) {
            return; // at the edge of the cell on that axis: nothing to do
        }
        LoaderHooks.sendToServer(CornerMovePayload.ofMask(pos, SELECTION.mask(), move.axis(), move.delta()));
        playTick(mc, move.delta() > 0 ? 1.2F : 1.0F);
    }

    /** A quiet UI click; the server's sync is silent for single moves so a held key does not rattle. */
    private static void playTick(Minecraft mc, float pitch) {
        if (FlexiCatClientConfig.moveSounds()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F));
        }
    }

    private static boolean isGroupModifierDown(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
    }

    /** Presses are read as held state, so vanilla's click counter is never used; keep it empty. */
    private static void drainClicks() {
        for (KeyMapping key : FlexiCatKeys.all()) {
            while (key.consumeClick()) {
                // discard
            }
        }
    }

    /** Rebuild the repeater if the configured timing changed (config screen / reload). */
    private static void refreshRepeater() {
        int delay = FlexiCatClientConfig.repeatDelayTicks();
        int interval = FlexiCatClientConfig.repeatIntervalTicks();
        if (delay != repeaterDelay || interval != repeaterInterval) {
            repeater = newRepeater();
        }
    }

    private static HeldKeyRepeater newRepeater() {
        repeaterDelay = FlexiCatClientConfig.repeatDelayTicks();
        repeaterInterval = FlexiCatClientConfig.repeatIntervalTicks();
        return new HeldKeyRepeater(FlexiCatKeys.moves().size(), repeaterDelay, repeaterInterval);
    }

    private static Component hud(CornerShape shape) {
        Corner corner = SELECTION.single();
        if (SELECTION.isEmpty()) {
            return Component.translatable("message.flexicat.hud.aim");
        }
        Component what;
        if (corner != null) {
            Vec3i16 p = shape.position(corner);
            what = Component.translatable("message.flexicat.hud.corner",
                    Component.translatable("corner.flexicat." + corner.name().toLowerCase(Locale.ROOT)),
                    p.x(), p.y(), p.z());
        } else {
            what = Component.translatable("message.flexicat.hud.group", SELECTION.count());
        }
        return Component.translatable("message.flexicat.hud.selected", what,
                FlexiCatKeys.MOVE_UP.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_DOWN.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_LEFT.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_RIGHT.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_AWAY.getTranslatedKeyMessage(),
                FlexiCatKeys.MOVE_TOWARDS.getTranslatedKeyMessage());
    }
}