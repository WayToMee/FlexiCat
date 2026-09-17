package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CornerMovePayloadTest {

    @Test
    void roundTripsThroughStreamCodec() {
        CornerMovePayload original = CornerMovePayload.of(new BlockPos(10, -3, 1000), Corner.UP_EAST_NORTH, Axis.Y, -1);
        ByteBuf buf = Unpooled.buffer();
        CornerMovePayload.STREAM_CODEC.encode(buf, original);
        CornerMovePayload decoded = CornerMovePayload.STREAM_CODEC.decode(buf);
        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes(), "nothing left over");
        assertEquals(List.of(Corner.UP_EAST_NORTH), decoded.corners());
        assertEquals(Axis.Y, decoded.axis());
    }

    @Test
    void carriesAGroupAsAMask() {
        int top = Corner.mask(List.of(Corner.UP_WEST_NORTH, Corner.UP_EAST_NORTH, Corner.UP_WEST_SOUTH, Corner.UP_EAST_SOUTH));
        CornerMovePayload payload = CornerMovePayload.ofMask(BlockPos.ZERO, top, Axis.Y, -1);
        assertTrue(payload.isValid());
        assertEquals(List.of(Corner.UP_WEST_NORTH, Corner.UP_EAST_NORTH, Corner.UP_WEST_SOUTH, Corner.UP_EAST_SOUTH),
                payload.corners(), "index order");
        ByteBuf buf = Unpooled.buffer();
        CornerMovePayload.STREAM_CODEC.encode(buf, payload);
        assertEquals(payload, CornerMovePayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void validatesMaskAxisAndStep() {
        BlockPos pos = BlockPos.ZERO;
        assertTrue(new CornerMovePayload(pos, 1, 0, 1).isValid());
        assertTrue(new CornerMovePayload(pos, Corner.ALL_MASK, 2, -1).isValid());
        assertFalse(new CornerMovePayload(pos, 0, 0, 1).isValid(), "empty selection");
        assertFalse(new CornerMovePayload(pos, Corner.ALL_MASK + 1, 0, 1).isValid(), "bit beyond the eight corners");
        assertFalse(new CornerMovePayload(pos, -1, 0, 1).isValid(), "negative mask");
        assertFalse(new CornerMovePayload(pos, 1, 3, 1).isValid(), "axis out of range");
        assertFalse(new CornerMovePayload(pos, 1, 0, 2).isValid(), "only single steps");
        assertFalse(new CornerMovePayload(pos, 1, 0, 0).isValid(), "zero step");
    }

    @Test
    void typeIsRegisteredUnderTheModNamespace() {
        assertEquals("flexicat", CornerMovePayload.TYPE.id().getNamespace());
        assertEquals("corner_move", CornerMovePayload.TYPE.id().getPath());
    }
}