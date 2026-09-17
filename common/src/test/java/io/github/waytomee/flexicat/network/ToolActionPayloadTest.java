package io.github.waytomee.flexicat.network;

import io.github.waytomee.flexicat.geometry.Axis;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolActionPayloadTest {

    @Test
    void roundTripsThroughStreamCodec() {
        for (ToolActionPayload original : new ToolActionPayload[] {
                ToolActionPayload.of(new BlockPos(-5, 70, 12), ToolActionPayload.Action.PASTE),
                ToolActionPayload.mirror(new BlockPos(1, 2, 3), Axis.Z),
                ToolActionPayload.rotate(new BlockPos(1, 2, 3), 3)}) {
            ByteBuf buf = Unpooled.buffer();
            ToolActionPayload.STREAM_CODEC.encode(buf, original);
            ToolActionPayload decoded = ToolActionPayload.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
            assertEquals(0, buf.readableBytes());
            assertTrue(decoded.isValid());
        }
        assertEquals(Axis.Z, ToolActionPayload.mirror(BlockPos.ZERO, Axis.Z).mirrorAxis());
    }

    @Test
    void rotateWrapsQuarterTurns() {
        assertEquals(3, ToolActionPayload.rotate(BlockPos.ZERO, -1).argument());
        assertEquals(1, ToolActionPayload.rotate(BlockPos.ZERO, 5).argument());
        assertFalse(ToolActionPayload.rotate(BlockPos.ZERO, 4).isValid(), "a full turn is a no-op and is rejected");
    }

    @Test
    void rejectsUnknownActionsAndBadArguments() {
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 0, 0).isValid());
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 1, 0).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, 0, 1).isValid(), "copy takes no argument");
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 2, 2).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, 2, 3).isValid(), "no fourth axis");
        assertTrue(new ToolActionPayload(BlockPos.ZERO, 3, 1).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, 3, 0).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, 4, 0).isValid());
        assertFalse(new ToolActionPayload(BlockPos.ZERO, -1, 0).isValid());
    }

    @Test
    void typeIsRegisteredUnderTheModNamespace() {
        assertEquals("flexicat:tool_action", ToolActionPayload.TYPE.id().toString());
    }
}