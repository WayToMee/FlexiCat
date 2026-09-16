package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

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
        assertEquals(Corner.UP_EAST_NORTH, decoded.corner());
        assertEquals(Axis.Y, decoded.axis());
    }

    @Test
    void validatesCornerAxisAndStep() {
        BlockPos pos = BlockPos.ZERO;
        assertTrue(new CornerMovePayload(pos, 0, 0, 1).isValid());
        assertTrue(new CornerMovePayload(pos, 7, 2, -1).isValid());
        assertFalse(new CornerMovePayload(pos, 8, 0, 1).isValid(), "corner out of range");
        assertFalse(new CornerMovePayload(pos, -1, 0, 1).isValid(), "negative corner");
        assertFalse(new CornerMovePayload(pos, 0, 3, 1).isValid(), "axis out of range");
        assertFalse(new CornerMovePayload(pos, 0, 0, 2).isValid(), "only single steps");
        assertFalse(new CornerMovePayload(pos, 0, 0, 0).isValid(), "zero step");
    }

    @Test
    void typeIsRegisteredUnderTheModNamespace() {
        assertEquals("flexicat", CornerMovePayload.TYPE.id().getNamespace());
        assertEquals("corner_move", CornerMovePayload.TYPE.id().getPath());
    }
}