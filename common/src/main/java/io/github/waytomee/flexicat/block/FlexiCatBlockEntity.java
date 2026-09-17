package io.github.waytomee.flexicat.block;

import io.github.waytomee.flexicat.codec.CornerShapeCodecs;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;

/**
 * Holds the {@link CornerShape} of one placed FlexiCat block.
 *
 * <p>The server owns the shape. {@link #setShape} is the single write path: it
 * marks the chunk dirty and pushes the new shape to clients through the block
 * entity's update packet. Clients only ever receive shapes, never invent them.
 *
 * <p>Loader modules may subclass this to plug the shape into their rendering
 * pipeline (model data, chunk re-meshing); {@link #onShapeSyncedOnClient()} is the
 * hook for that.
 */
public class FlexiCatBlockEntity extends BlockEntity {

    private CornerShape shape = CornerShape.cube();
    private VoxelShape voxelShape;
    private VoxelShape collisionShape;

    public FlexiCatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CornerShape shape() {
        return shape;
    }

    /**
     * Collision and outline shape derived from the actual faces (see
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
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
        return true;
    }

    /**
     * Called on the logical client after a shape arrived from the server (chunk
     * load or block-entity update packet) and differs from the one held before.
     * The base implementation re-meshes the surrounding chunk sections; loader
     * subclasses add whatever their model pipeline needs (call {@code super}).
     */
    protected void onShapeSyncedOnClient() {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CornerShape loaded = CornerShapeCodecs.read(tag);
        boolean changed = !loaded.equals(shape);
        shape = loaded;
        invalidateShapes();
        Level level = getLevel();
        if (changed && level != null && level.isClientSide()) {
            onShapeSyncedOnClient();
        }
    }

    // --- client sync ---------------------------------------------------------------

    /** Full state for chunk loads: the shape is all there is. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}