package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server intent: "move corner {@code cornerIndex} of the block at
 * {@code pos} along {@code axisOrdinal} by {@code delta} grid steps".
 *
 * <p>This is an <em>intent</em>, not a shape: the server validates it against
 * the live block and applies the clamped move itself (see
 * {@code CornerEditServer}). The fields are raw integers so a malformed packet
 * is rejected by {@link #isValid()} instead of throwing while decoding.
 *
 * <p>Registered under {@code flexicat:corner_move}; registration itself is
 * loader-specific.
 */
public record CornerMovePayload(BlockPos pos, int cornerIndex, int axisOrdinal, int delta)
        implements CustomPacketPayload {

    public static final Type<CornerMovePayload> TYPE = new Type<>(FlexiCat.id("corner_move"));

    public static final StreamCodec<ByteBuf, CornerMovePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CornerMovePayload::pos,
            ByteBufCodecs.VAR_INT, CornerMovePayload::cornerIndex,
            ByteBufCodecs.VAR_INT, CornerMovePayload::axisOrdinal,
            ByteBufCodecs.VAR_INT, CornerMovePayload::delta,
            CornerMovePayload::new);

    public static CornerMovePayload of(BlockPos pos, Corner corner, Axis axis, int delta) {
        return new CornerMovePayload(pos.immutable(), corner.index(), axis.ordinal(), delta);
    }

    /** {@code true} if the corner and axis exist and the delta is a single grid step. */
    public boolean isValid() {
        return cornerIndex >= 0 && cornerIndex < Corner.COUNT
                && axisOrdinal >= 0 && axisOrdinal < Axis.values().length
                && (delta == 1 || delta == -1);
    }

    /** Only meaningful when {@link #isValid()} is true. */
    public Corner corner() {
        return Corner.byIndex(cornerIndex);
    }

    /** Only meaningful when {@link #isValid()} is true. */
    public Axis axis() {
        return Axis.byOrdinal(axisOrdinal);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}