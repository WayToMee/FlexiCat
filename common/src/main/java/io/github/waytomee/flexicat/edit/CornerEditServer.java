package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.item.CornerToolItem;
import io.github.waytomee.flexicat.network.CornerMovePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
        if (!CornerToolItem.isHeldBy(player) || !player.mayBuild()) {
            return false;
        }
        BlockPos pos = payload.pos();
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos)
                || !player.canInteractWithBlock(pos, REACH_PADDING)
                || !level.mayInteract(player, pos)) {
            return false;
        }
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, pos);
        if (target.isEmpty()) {
            return false;
        }
        FlexiCatBlockEntity be = target.get();
        CornerShape next = be.shape().move(payload.corner(), payload.axis(), payload.delta());
        return be.setShape(next);
    }
}