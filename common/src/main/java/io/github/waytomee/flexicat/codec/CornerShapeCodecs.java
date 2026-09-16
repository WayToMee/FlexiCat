package io.github.waytomee.flexicat.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.nio.ByteBuffer;

/**
 * Minecraft-facing encodings of {@link CornerShape}. All of them wrap the
 * 24-byte form defined by the geometry core, so a shape written by any of them
 * can be understood by the others and by future versions.
 *
 * <ul>
 *   <li>{@link #CODEC} — DataFixerUpper codec (NBT, JSON, data components).</li>
 *   <li>{@link #STREAM_CODEC} — network encoding for packets.</li>
 *   <li>{@link #write} / {@link #read} — block-entity NBT under one fixed key.</li>
 * </ul>
 */
public final class CornerShapeCodecs {

    /** Key of the shape inside a FlexiCat block entity's NBT. Part of the save format. */
    public static final String NBT_KEY = "Shape";

    public static final Codec<CornerShape> CODEC = Codec.BYTE_BUFFER.comapFlatMap(
            CornerShapeCodecs::decode,
            shape -> ByteBuffer.wrap(shape.toBytes()));

    public static final StreamCodec<ByteBuf, CornerShape> STREAM_CODEC = ByteBufCodecs
            .byteArray(CornerShape.BYTES)
            .map(CornerShape::fromBytes, CornerShape::toBytes);

    private CornerShapeCodecs() {
    }

    private static DataResult<CornerShape> decode(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        if (bytes.length != CornerShape.BYTES) {
            return DataResult.error(() -> "CornerShape needs " + CornerShape.BYTES + " bytes, got " + bytes.length);
        }
        return DataResult.success(CornerShape.fromBytes(bytes));
    }

    /** Store the shape under {@link #NBT_KEY}. The undeformed cube is written too, so saves are explicit. */
    public static void write(CompoundTag tag, CornerShape shape) {
        tag.putByteArray(NBT_KEY, shape.toBytes());
    }

    /**
     * Read the shape stored under {@link #NBT_KEY}.
     *
     * <p>Missing data yields the undeformed cube (older or foreign data should never
     * crash a world load); data of the wrong length is logged and also treated as a
     * cube. Out-of-range coordinates are clamped by the geometry core.
     */
    public static CornerShape read(CompoundTag tag) {
        if (!tag.contains(NBT_KEY, Tag.TAG_BYTE_ARRAY)) {
            return CornerShape.cube();
        }
        byte[] bytes = tag.getByteArray(NBT_KEY);
        if (bytes.length != CornerShape.BYTES) {
            FlexiCat.LOGGER.warn("Ignoring corner shape with {} bytes (expected {}); using a cube", bytes.length, CornerShape.BYTES);
            return CornerShape.cube();
        }
        return CornerShape.fromBytes(bytes);
    }
}