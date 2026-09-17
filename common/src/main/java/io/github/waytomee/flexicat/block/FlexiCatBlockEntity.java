package io.github.waytomee.flexicat.block;

import io.github.waytomee.flexicat.codec.CornerShapeCodecs;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Holds the {@link CornerShape} and the optional material of one placed FlexiCat block.
 *
 * <p>The server owns both. {@link #setShape} and {@link #setMaterial} are the only
 * write paths: they mark the chunk dirty and push the new state to clients through
 * the block entity's update packet. Clients only ever receive state, never invent it.
 *
 * <p>Loader modules may subclass this to plug the state into their rendering
 * pipeline (model data, chunk re-meshing); {@link #onSyncedOnClient()} is the hook
 * for that.
 */
public class FlexiCatBlockEntity extends BlockEntity {

    /** NBT key of the material block state, {@code NbtUtils.writeBlockState} format. */
    public static final String MATERIAL_KEY = "Material";

    private CornerShape shape = CornerShape.cube();
    @Nullable
    private BlockState material;
    private VoxelShape voxelShape;
    private VoxelShape collisionShape;

    public FlexiCatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CornerShape shape() {
        return shape;
    }

    /** The block whose look this block borrows, if it has been filled. */
    public Optional<BlockState> material() {
        return Optional.ofNullable(material);
    }

    /**
     * Picking / outline shape derived from the actual faces (see
     * {@link FlexiCatShapes}), cached until the shape changes.
     */
    public VoxelShape voxelShape() {
        if (voxelShape == null) {
            voxelShape = FlexiCatShapes.of(shape);
        }
        return voxelShape;
    }

    /** Coarser entity-collision shape (see {@link FlexiCatShapes#collisionOf}), cached until the shape changes. */
    public VoxelShape collisionShape() {
        if (collisionShape == null) {
            collisionShape = FlexiCatShapes.collisionOf(shape);
        }
        return collisionShape;
    }

    private void invalidateShapes() {
        voxelShape = null;
        collisionShape = null;
    }

    /**
     * Replace the shape. Intended for the logical server; on a client it only
     * updates the local copy (used when applying a sync packet).
     *
     * @return {@code true} if the shape actually changed
     */
    public boolean setShape(CornerShape newShape) {
        Objects.requireNonNull(newShape, "shape");
        if (newShape.equals(shape)) {
            return false;
        }
        shape = newShape;
        invalidateShapes();
        sync();
        return true;
    }

    /**
     * Fill with (or, for {@code null}, empty) a material. Callers check
     * {@link FlexiCatMaterials#isAcceptable} first; this only stores it.
     *
     * @return {@code true} if the material actually changed
     */
    public boolean setMaterial(@Nullable BlockState newMaterial) {
        if (Objects.equals(newMaterial, material)) {
            return false;
        }
        material = newMaterial;
        sync();
        return true;
    }

    private void sync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * Called on the logical client after a shape or material arrived from the server
     * (chunk load or block-entity update packet) and differs from what was held before.
     * The base implementation re-meshes the surrounding chunk sections; loader
     * subclasses add whatever their model pipeline needs (call {@code super}).
     */
    protected void onSyncedOnClient() {
        Level level = getLevel();
        if (level != null && level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // --- persistence ---------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CornerShapeCodecs.write(tag, shape);
        if (material != null) {
            tag.put(MATERIAL_KEY, NbtUtils.writeBlockState(material));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CornerShape loadedShape = CornerShapeCodecs.read(tag);
        BlockState loadedMaterial = null;
        if (tag.contains(MATERIAL_KEY, CompoundTag.TAG_COMPOUND)) {
            BlockState read = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(MATERIAL_KEY));
            loadedMaterial = read.isAir() ? null : read; // unknown block (removed mod) reads as air: treat as empty
        }
        boolean changed = !loadedShape.equals(shape) || !Objects.equals(loadedMaterial, material);
        shape = loadedShape;
        material = loadedMaterial;
        invalidateShapes();
        Level level = getLevel();
        if (changed && level != null && level.isClientSide()) {
            onSyncedOnClient();
        }
    }

    // --- client sync ---------------------------------------------------------------

    /** Full state for chunk loads: shape and material are all there is. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}