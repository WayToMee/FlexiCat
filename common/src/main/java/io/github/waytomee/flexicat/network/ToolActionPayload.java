package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.FlexiCat;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server intent for the corner tool's whole-shape actions on the block at
 * {@code pos}: copy its shape onto the tool, or paste the tool's shape onto it.
 *
 * <p>Like {@link CornerMovePayload} this carries no shape data — the server reads the
 * shape from the block or from the tool in the player's hand, so a client cannot
 * fabricate one. Registered under {@code flexicat:tool_action}.
 */
public record ToolActionPayload(BlockPos pos, int actionOrdinal) implements CustomPacketPayload {

    public enum Action {
        /** Store the block's shape on the held tool. */
        COPY,
        /** Replace the block's shape with the one stored on the held tool. */
        PASTE;

        private static final Action[] VALUES = values();
    }

    public static final Type<ToolActionPayload> TYPE = new Type<>(FlexiCat.id("tool_action"));

    public static final StreamCodec<ByteBuf, ToolActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToolActionPayload::pos,
            ByteBufCodecs.VAR_INT, ToolActionPayload::actionOrdinal,
            ToolActionPayload::new);

    public static ToolActionPayload of(BlockPos pos, Action action) {
        return new ToolActionPayload(pos.immutable(), action.ordinal());
    }

    public boolean isValid() {
        return actionOrdinal >= 0 && actionOrdinal < Action.VALUES.length;
    }

    /** Only meaningful when {@link #isValid()} is true. */
    public Action action() {
        return Action.VALUES[actionOrdinal];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}