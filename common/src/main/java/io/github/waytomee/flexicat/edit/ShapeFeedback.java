package io.github.waytomee.flexicat.edit;

import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side sound and particles for a whole-shape change (paste, reset). Corner
 * moves are frequent and get only a quiet client-side tick instead (see
 * {@code CornerEditClient}). Everything is themed by the block's material when it has
 * one, so a filled block sounds and dusts like what it is made of.
 */
public final class ShapeFeedback {

    private ShapeFeedback() {
    }

    /** The material's sound type, or the FlexiCat block's own for an empty block. */
    public static SoundType soundOf(FlexiCatBlockEntity be) {
        return be.material().map(BlockState::getSoundType).orElseGet(() -> be.getBlockState().getSoundType());
    }

    /** Play the "placed" sound and a puff of block particles at the shape's centre. */
    public static void shapeChanged(ServerLevel level, BlockPos pos, FlexiCatBlockEntity be) {
        SoundType sound = soundOf(be);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.9F);

        BlockState dust = be.material().orElseGet(be::getBlockState);
        int[] b = be.shape().bounds();
        double g = CornerShape.GRID;
        double cx = pos.getX() + (b[0] + b[1]) / (2.0 * g);
        double cy = pos.getY() + (b[2] + b[3]) / (2.0 * g);
        double cz = pos.getZ() + (b[4] + b[5]) / (2.0 * g);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dust),
                cx, cy, cz, 12,
                (b[1] - b[0]) / (2.0 * g), (b[3] - b[2]) / (2.0 * g), (b[5] - b[4]) / (2.0 * g),
                0.05);
    }
}