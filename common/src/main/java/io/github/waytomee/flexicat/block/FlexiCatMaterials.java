package io.github.waytomee.flexicat.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks may fill a FlexiCat block (stage 6, copycat behaviour).
 *
 * <p>The material only lends its <em>look</em> (textures, tint, render layer, particles,
 * sounds); the FlexiCat block keeps its own shape, collision and behaviour. That
 * only works for blocks whose look is a plain textured cube, so a material must
 * <ul>
 *   <li>render as a model (no entity renderers, no invisible blocks),</li>
 *   <li>have no block entity (chests, signs, banners carry state we cannot show),</li>
 *   <li>collide as a full cube (stairs, fences and slabs have no texture for the
 *       places our faces would sample),</li>
 *   <li>not be a FlexiCat block itself.</li>
 * </ul>
 * The default block state of the item's block is used; orientation is not kept.
 */
public final class FlexiCatMaterials {

    private FlexiCatMaterials() {
    }

    public static boolean isAcceptable(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof FlexiCatBlock) {
            return false;
        }
        if (state.getRenderShape() != RenderShape.MODEL || state.hasBlockEntity()) {
            return false;
        }
        return Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}