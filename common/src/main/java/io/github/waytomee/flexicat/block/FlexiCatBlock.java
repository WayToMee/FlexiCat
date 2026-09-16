package io.github.waytomee.flexicat.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The FlexiCat block. It has no block-state properties: everything that makes one
 * placed block differ from another lives in its {@link FlexiCatBlockEntity}.
 *
 * <p>The visual mesh is produced by the loader's client module from the block
 * entity's shape (stage 4). Outline and collision currently use the axis-aligned
 * bounds of the corners; exact per-face collision and ray casting are stage 5.
 */
public class FlexiCatBlock extends BaseEntityBlock {

    private final Supplier<BlockEntityType<FlexiCatBlockEntity>> entityType;

    public FlexiCatBlock(Properties properties, Supplier<BlockEntityType<FlexiCatBlockEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    /**
     * Properties shared by every loader. Occlusion is off because the final shape
     * is rarely a full cube; light and vision are decided by the shape later.
     */
    public static Properties defaultProperties() {
        return Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(1.5F, 6.0F)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false);
    }

    @Override
    protected MapCodec<? extends FlexiCatBlock> codec() {
        return simpleCodec(properties -> new FlexiCatBlock(properties, entityType));
    }

    /**
     * The registered type decides the concrete block-entity class, so a loader
     * module can register a subclass (e.g. with model-data support) without
     * touching common code.
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entityType.get().create(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return entityAt(level, pos).map(FlexiCatBlockEntity::boundsShape).orElse(Shapes.block());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** The block entity at {@code pos}, if it is a FlexiCat one. */
    public static Optional<FlexiCatBlockEntity> entityAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FlexiCatBlockEntity be ? Optional.of(be) : Optional.empty();
    }
}