package io.github.waytomee.flexicat.block;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlexiCatMaterialsTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void plainFullCubesAreAccepted() {
        assertTrue(FlexiCatMaterials.isAcceptable(Blocks.STONE.defaultBlockState()));
        assertTrue(FlexiCatMaterials.isAcceptable(Blocks.OAK_PLANKS.defaultBlockState()));
        assertTrue(FlexiCatMaterials.isAcceptable(Blocks.GLASS.defaultBlockState()));
        assertTrue(FlexiCatMaterials.isAcceptable(Blocks.GRASS_BLOCK.defaultBlockState()), "tinted cubes are fine");
        assertTrue(FlexiCatMaterials.isAcceptable(Blocks.OAK_LOG.defaultBlockState()), "default orientation is used");
    }

    @Test
    void nonCubesBlockEntitiesAndInvisiblesAreRejected() {
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.AIR.defaultBlockState()));
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.OAK_STAIRS.defaultBlockState()));
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.STONE_SLAB.defaultBlockState()));
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.OAK_FENCE.defaultBlockState()));
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.CHEST.defaultBlockState()), "block entity");
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.BARRIER.defaultBlockState()), "invisible");
        assertFalse(FlexiCatMaterials.isAcceptable(Blocks.WATER.defaultBlockState()));
    }

    // The "not a FlexiCat block itself" rule is an instanceof check; a FlexiCat block
    // cannot be constructed outside registry bootstrap (intrusive holders), so it is
    // covered by the dev-server smoke test rather than here.
}