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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The FlexiCat block. It has no block-state properties: everything that makes one
 * placed block differ from another lives in its {@link FlexiCatBlockEntity}.
 *
 * <p>Stage 2 renders the block as a plain full cube from a normal JSON model. The
 * shape-driven mesh, collision and ray casting come in later stages, all reading
 * from the block entity.
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlexiCatBlockEntity(entityType.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The block entity at {@code pos}, if it is a FlexiCat one. */
    public static Optional<FlexiCatBlockEntity> entityAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FlexiCatBlockEntity be ? Optional.of(be) : Optional.empty();
    }
}