package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * NeoForge flavour of the block entity: exposes the shape as {@link ModelData} so
 * the chunk mesher can hand it to {@link io.github.waytomee.flexicat.neoforge.client.FlexiCatBakedModel},
 * and asks for a model-data refresh whenever a new shape arrives from the server.
 */
public class NeoForgeFlexiCatBlockEntity extends FlexiCatBlockEntity {

    public NeoForgeFlexiCatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(FlexiCatModelProperties.SHAPE, shape());
    }

    @Override
    protected void onShapeSyncedOnClient() {
        requestModelDataUpdate();
        super.onShapeSyncedOnClient();
    }
}