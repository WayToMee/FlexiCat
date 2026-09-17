package io.github.waytomee.flexicat.block;

import com.mojang.serialization.MapCodec;
import io.github.waytomee.flexicat.item.CornerToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The FlexiCat block. It has no block-state properties: everything that makes one
 * placed block differ from another (shape, material) lives in its
 * {@link FlexiCatBlockEntity}.
 *
 * <p>The visual mesh is produced by the loader's client module from the block
 * entity's shape and material (stages 4 and 6). Outline and ray casting use a 1/16
 * voxel approximation of the actual faces, entity collision a 4/16 one (stage 5,
 * {@link FlexiCatShapes}); the loader's client module draws the true edges as the
 * hit outline.
 *
 * <p>Filling (stage 6): right-click an empty FlexiCat block with an acceptable block
 * item (see {@link FlexiCatMaterials}) to make it look like that block; one item is
 * consumed. Sneak + right-click with an empty hand takes the material back out. The
 * material is dropped when the block is broken.
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
     * {@code dynamicShape} is essential: without it vanilla caches the collision
     * shape per block state at startup (with no block entity in reach) and would
     * treat every FlexiCat block as a full cube for collision.
     */
    public static Properties defaultProperties() {
        return Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .strength(1.5F, 6.0F)
                .noOcclusion()
                .dynamicShape()
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
        return entityAt(level, pos).map(FlexiCatBlockEntity::voxelShape).orElse(Shapes.block());
    }

    /** Entities collide with a coarser version of the hull (4/16 steps), so slopes are walkable. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return entityAt(level, pos).map(FlexiCatBlockEntity::collisionShape).orElse(Shapes.block());
    }

    // --- light ---------------------------------------------------------------------

    /**
     * A deformed block lets light through like a stair or slab (light block 0); an
     * undeformed cube blocks it like its material would, or completely when unfilled.
     *
     * <p>Vanilla's defaults would give the cube a light block of {@code 1} (no
     * occlusion + full shape): the block's own cell ends up one level darker than its
     * surroundings, and since deformed faces sample light at that cell, two blocks in
     * the same plane rendered with different brightness depending on when the light
     * engine last looked at them. {@link FlexiCatBlockEntity} asks for a relight whenever
     * the shape crosses the cube boundary, because a block-entity change alone never does.
     */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        Optional<FlexiCatBlockEntity> be = entityAt(level, pos);
        if (be.isPresent() && !be.get().shape().isCube()) {
            return 0;
        }
        return be.flatMap(FlexiCatBlockEntity::material)
                .map(material -> material.getLightBlock(level, pos))
                .orElse(level.getMaxLightLevel());
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        Optional<FlexiCatBlockEntity> be = entityAt(level, pos);
        if (be.isPresent() && !be.get().shape().isCube()) {
            return true;
        }
        return be.flatMap(FlexiCatBlockEntity::material)
                .map(material -> material.propagatesSkylightDown(level, pos))
                .orElse(false);
    }

    // --- filling (stage 6) ---------------------------------------------------------

    /**
     * A block item on an unfilled block fills it. Anything else passes through, so the
     * corner tool's own {@code useOn} still runs and other items behave as in vanilla.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || stack.getItem() instanceof CornerToolItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Optional<FlexiCatBlockEntity> target = entityAt(level, pos);
        if (target.isEmpty() || target.get().material().isPresent()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState material = blockItem.getBlock().defaultBlockState();
        if (!FlexiCatMaterials.isAcceptable(material)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (target.get().setMaterial(material)) {
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            SoundType sound = material.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
        return ItemInteractionResult.CONSUME;
    }

    /** Sneak + right-click with an empty hand takes the material back out. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Optional<FlexiCatBlockEntity> target = entityAt(level, pos);
        if (target.isEmpty() || target.get().material().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockState material = target.get().material().get();
        target.get().setMaterial(null);
        if (!player.hasInfiniteMaterials()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(material.getBlock()));
        }
        SoundType sound = material.getSoundType();
        level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.CONSUME;
    }

    /** The material is a separate item: drop it alongside the block's own loot. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            entityAt(level, pos).flatMap(FlexiCatBlockEntity::material)
                    .ifPresent(material -> Block.popResource(level, pos, new ItemStack(material.getBlock())));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The block entity at {@code pos}, if it is a FlexiCat one. */
    public static Optional<FlexiCatBlockEntity> entityAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FlexiCatBlockEntity be ? Optional.of(be) : Optional.empty();
    }
}