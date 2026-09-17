package io.github.waytomee.flexicat.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolActionPayloadTest {

    @Test
    void roundTripsThroughStreamCodec() {
        ToolActionPayload original = ToolActionPayload.of(new BlockPos(-5, 70, 12), ToolActionPayload.Action.PASTE);
        ByteBuf buf = Unpooled.buffer();
        ToolActionPayload.STREAM_CODEC.encode(buf, original);
        ToolActionPayload decoded = ToolActionPayload.STREAM_CODEC.decode(buf);
        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes());
        assertEquals(ToolActionPayload.Action.PASTE, decoded.action());
    }

    @Test
    void rejectsUnknownActions() {
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 0).isValid());
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 1).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, 2).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, -1).isValid());
    }

    @Test
    void typeIsRegisteredUnderTheModNamespace() {
        assertEquals("flexicat:tool_action", ToolActionPayload.TYPE.id().toString());
    }
}