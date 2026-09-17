package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Client → server intent: "move the corners in {@code cornerMask} of the block at
 * {@code pos} along {@code axisOrdinal} by {@code delta} grid steps, as one group".
 *
 * <p>This is an <em>intent</em>, not a shape: the server validates it against
 * the live block and applies the clamped group move itself (see
 * {@code CornerEditServer}). The fields are raw integers so a malformed packet
 * is rejected by {@link #isValid()} instead of throwing while decoding.
 *
 * <p>Registered under {@code flexicat:corner_move}; registration itself is
 * loader-specific. Protocol 2: the corner field became a mask (stage 7, group
 * movement); a single corner is simply a mask with one bit.
 */
public record CornerMovePayload(BlockPos pos, int cornerMask, int axisOrdinal, int delta)
        implements CustomPacketPayload {

    public static final Type<CornerMovePayload> TYPE = new Type<>(FlexiCat.id("corner_move"));

    public static final StreamCodec<ByteBuf, CornerMovePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CornerMovePayload::pos,
            ByteBufCodecs.VAR_INT, CornerMovePayload::cornerMask,
            ByteBufCodecs.VAR_INT, CornerMovePayload::axisOrdinal,
            ByteBufCodecs.VAR_INT, CornerMovePayload::delta,
            CornerMovePayload::new);

    public static CornerMovePayload of(BlockPos pos, Corner corner, Axis axis, int delta) {
        return new CornerMovePayload(pos.immutable(), corner.bit(), axis.ordinal(), delta);
    }

    public static CornerMovePayload ofMask(BlockPos pos, int cornerMask, Axis axis, int delta) {
        return new CornerMovePayload(pos.immutable(), cornerMask, axis.ordinal(), delta);
    }

    /** {@code true} if at least one corner is named, the axis exists and the delta is a single grid step. */
    public boolean isValid() {
        return Corner.isValidMask(cornerMask)
                && axisOrdinal >= 0 && axisOrdinal < Axis.values().length
                && (delta == 1 || delta == -1);
    }

    /** Only meaningful when {@link #isValid()} is true. */
    public List<Corner> corners() {
        return Corner.fromMask(cornerMask);
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