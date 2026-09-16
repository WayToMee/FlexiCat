package io.github.waytomee.flexicat.codec;

import com.mojang.serialization.JsonOps;
import io.github.waytomee.flexicat.geometry.Axis;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The codecs are thin wrappers, but they define the save and network format, so
 * every path is pinned down here. These tests need no Minecraft bootstrap: NBT,
 * DataFixerUpper and Netty are plain libraries.
 */
class CornerShapeCodecsTest {

    private static CornerShape sample() {
        return CornerShape.cube()
                .move(Corner.UP_EAST_NORTH, Axis.Y, -8)
                .move(Corner.UP_WEST_NORTH, Axis.Y, -16)
                .move(Corner.DOWN_EAST_SOUTH, Axis.X, -3);
    }

    @Test
    void nbtRoundTrip() {
        CompoundTag tag = new CompoundTag();
        CornerShapeCodecs.write(tag, sample());
        assertTrue(tag.contains(CornerShapeCodecs.NBT_KEY, Tag.TAG_BYTE_ARRAY));
        assertEquals(CornerShape.BYTES, tag.getByteArray(CornerShapeCodecs.NBT_KEY).length);
        assertEquals(sample(), CornerShapeCodecs.read(tag));
    }

    @Test
    void nbtMissingOrBrokenFallsBackToCube() {
        assertSame(CornerShape.cube(), CornerShapeCodecs.read(new CompoundTag()));

        CompoundTag wrongLength = new CompoundTag();
        wrongLength.putByteArray(CornerShapeCodecs.NBT_KEY, new byte[] {1, 2, 3});
        assertSame(CornerShape.cube(), CornerShapeCodecs.read(wrongLength));

        CompoundTag wrongType = new CompoundTag();
        wrongType.putString(CornerShapeCodecs.NBT_KEY, "nope");
        assertSame(CornerShape.cube(), CornerShapeCodecs.read(wrongType));
    }

    @Test
    void nbtOutOfRangeIsClamped() {
        byte[] bytes = CornerShape.cube().toBytes();
        bytes[Corner.UP_EAST_SOUTH.index() * 3 + Axis.Y.ordinal()] = 40;
        bytes[Corner.DOWN_WEST_NORTH.index() * 3 + Axis.X.ordinal()] = -5;
        CompoundTag tag = new CompoundTag();
        tag.putByteArray(CornerShapeCodecs.NBT_KEY, bytes);
        CornerShape read = CornerShapeCodecs.read(tag);
        assertEquals(CornerShape.MAX, read.position(Corner.UP_EAST_SOUTH, Axis.Y));
        assertEquals(CornerShape.MIN, read.position(Corner.DOWN_WEST_NORTH, Axis.X));
    }

    @Test
    void codecRoundTripJsonAndNbt() {
        var json = CornerShapeCodecs.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow();
        assertEquals(sample(), CornerShapeCodecs.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

        var nbt = CornerShapeCodecs.CODEC.encodeStart(NbtOps.INSTANCE, sample()).getOrThrow();
        assertEquals(Tag.TAG_BYTE_ARRAY, nbt.getId());
        assertEquals(sample(), CornerShapeCodecs.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
    }

    @Test
    void codecRejectsWrongLength() {
        var result = CornerShapeCodecs.CODEC.parse(JsonOps.INSTANCE,
                CornerShapeCodecs.CODEC.encodeStart(JsonOps.INSTANCE, sample()).getOrThrow());
        assertTrue(result.isSuccess());

        var tooShort = new com.google.gson.JsonArray();
        for (int i = 0; i < 5; i++) tooShort.add(0);
        assertTrue(CornerShapeCodecs.CODEC.parse(JsonOps.INSTANCE, tooShort).isError());
    }

    @Test
    void streamCodecRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        try {
            CornerShapeCodecs.STREAM_CODEC.encode(buf, sample());
            // VarInt length prefix (1 byte for 24) + 24 payload bytes
            assertEquals(1 + CornerShape.BYTES, buf.readableBytes());
            assertEquals(sample(), CornerShapeCodecs.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void cubeStaysCanonical() {
        CompoundTag tag = new CompoundTag();
        CornerShapeCodecs.write(tag, CornerShape.cube());
        assertSame(CornerShape.cube(), CornerShapeCodecs.read(tag));
    }
}