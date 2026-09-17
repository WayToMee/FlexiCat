package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.geometry.Axis;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server intent for the corner tool's whole-shape actions on the block at
 * {@code pos}: copy its shape onto the tool, paste the tool's shape onto it, mirror it
 * across a cell axis, or rotate it about the vertical axis.
 *
 * <p>Like {@link CornerMovePayload} this carries no shape data — the server reads the
 * shape from the block or from the tool in the player's hand and applies the named
 * transform itself, so a client cannot fabricate a shape. {@code argument} is the
 * action's parameter: the {@link Axis} ordinal for {@link Action#MIRROR}, the number
 * of quarter turns ({@code 1..3}) for {@link Action#ROTATE}, {@code 0} otherwise.
 * Registered under {@code flexicat:tool_action}.
 */
public record ToolActionPayload(BlockPos pos, int actionOrdinal, int argument) implements CustomPacketPayload {

    public enum Action {
        /** Store the block's shape on the held tool. */
        COPY,
        /** Replace the block's shape with the one stored on the held tool. */
        PASTE,
        /** Mirror the block's shape across the cell axis given by {@code argument}. */
        MIRROR,
        /** Rotate the block's shape {@code argument} × 90° clockwise about Y (seen from above). */
        ROTATE;

        private static final Action[] VALUES = values();

        boolean acceptsArgument(int argument) {
            return switch (this) {
                case COPY, PASTE -> argument == 0;
                case MIRROR -> argument >= 0 && argument < Axis.values().length;
                case ROTATE -> argument >= 1 && argument <= 3;
            };
        }
    }

    public static final Type<ToolActionPayload> TYPE = new Type<>(FlexiCat.id("tool_action"));

    public static final StreamCodec<ByteBuf, ToolActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToolActionPayload::pos,
            ByteBufCodecs.VAR_INT, ToolActionPayload::actionOrdinal,
            ByteBufCodecs.VAR_INT, ToolActionPayload::argument,
            ToolActionPayload::new);

    /** An action without a parameter (copy / paste). */
    public static ToolActionPayload of(BlockPos pos, Action action) {
        return of(pos, action, 0);
    }

    public static ToolActionPayload of(BlockPos pos, Action action, int argument) {
        return new ToolActionPayload(pos.immutable(), action.ordinal(), argument);
    }

    public static ToolActionPayload mirror(BlockPos pos, Axis axis) {
        return of(pos, Action.MIRROR, axis.ordinal());
    }

    public static ToolActionPayload rotate(BlockPos pos, int quarterTurns) {
        return of(pos, Action.ROTATE, ((quarterTurns % 4) + 4) % 4);
    }

    public boolean isValid() {
        return actionOrdinal >= 0 && actionOrdinal < Action.VALUES.length
                && action().acceptsArgument(argument);
    }

    /** Only meaningful when {@code actionOrdinal} is in range. */
    public Action action() {
        return Action.VALUES[actionOrdinal];
    }

    /** The mirror axis; only meaningful for a valid {@link Action#MIRROR}. */
    public Axis mirrorAxis() {
        return Axis.byOrdinal(argument);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}