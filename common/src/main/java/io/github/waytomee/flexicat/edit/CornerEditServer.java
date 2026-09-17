package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.item.CornerToolItem;
import io.github.waytomee.flexicat.item.FlexiCatComponents;
import io.github.waytomee.flexicat.network.CornerMovePayload;
import io.github.waytomee.flexicat.network.ToolActionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Server-side authority for corner edits. The client only ever sends intents;
 * this is the single place where an intent becomes a shape change.
 */
public final class CornerEditServer {

    /** Extra reach beyond the vanilla block interaction range, matching the client's check. */
    public static final double REACH_PADDING = 1.0;

    private CornerEditServer() {
    }

    /**
     * Validate and apply a move intent. Must run on the server thread.
     *
     * @return {@code true} if the block's shape changed
     */
    public static boolean handleMove(ServerPlayer player, CornerMovePayload payload) {
        if (!payload.isValid()) {
            return false;
        }
        Optional<FlexiCatBlockEntity> target = targetOf(player, payload.pos());
        if (target.isEmpty()) {
            return false;
        }
        FlexiCatBlockEntity be = target.get();
        CornerShape next = be.shape().moveGroup(payload.corners(), payload.axis(), payload.delta());
        return be.setShape(next);
    }

    /**
     * Validate and apply a copy/paste intent. Must run on the server thread.
     *
     * @return {@code true} if something happened (shape copied, or shape changed)
     */
    public static boolean handleToolAction(ServerPlayer player, ToolActionPayload payload) {
        if (!payload.isValid()) {
            return false;
        }
        Optional<FlexiCatBlockEntity> target = targetOf(player, payload.pos());
        if (target.isEmpty()) {
            return false;
        }
        FlexiCatBlockEntity be = target.get();
        ItemStack tool = CornerToolItem.heldBy(player);
        return switch (payload.action()) {
            case COPY -> {
                tool.set(FlexiCatComponents.SHAPE, be.shape());
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.4F);
                player.displayClientMessage(Component.translatable("message.flexicat.shape_copied"), true);
                yield true;
            }
            case PASTE -> {
                CornerShape shape = tool.get(FlexiCatComponents.SHAPE);
                if (shape == null) {
                    player.displayClientMessage(Component.translatable("message.flexicat.tool_empty"), true);
                    yield false;
                }
                boolean changed = be.setShape(shape);
                if (changed) {
                    ShapeFeedback.shapeChanged(player.serverLevel(), payload.pos(), be);
                }
                player.displayClientMessage(Component.translatable(changed
                        ? "message.flexicat.shape_pasted" : "message.flexicat.shape_same"), true);
                yield changed;
            }
        };
    }

    /**
     * The FlexiCat block entity at {@code pos} if this player may edit it right now:
     * tool in hand, allowed to build, block loaded and in reach.
     */
    private static Optional<FlexiCatBlockEntity> targetOf(ServerPlayer player, BlockPos pos) {
        if (!CornerToolItem.isHeldBy(player) || !player.mayBuild()) {
            return Optional.empty();
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)
                || !player.canInteractWithBlock(pos, REACH_PADDING)
                || !level.mayInteract(player, pos)) {
            return Optional.empty();
        }
        return FlexiCatBlock.entityAt(level, pos);
    }
}