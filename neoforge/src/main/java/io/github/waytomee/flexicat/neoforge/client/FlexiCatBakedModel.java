package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.CubeFace;
import io.github.waytomee.flexicat.geometry.ShapeMesh;
import io.github.waytomee.flexicat.neoforge.FlexiCatModelProperties;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the FlexiCat block's quads at chunk-mesh time from the {@link CornerShape}
 * delivered through {@link ModelData}.
 *
 * <p>Wraps the JSON-baked placeholder model: that model still supplies the
 * particle sprite, the texture, and the item rendering, and is used unchanged when
 * no shape is available (e.g. no block entity in reach of the mesher).
 *
 * <p>Full, undeformed faces are returned for their {@link Direction} so vanilla
 * culls them against opaque neighbours exactly like a cube. Every deformed face is
 * returned as an unculled quad ({@code side == null}) and tagged with the direction
 * closest to its real normal for directional shading.
 */
public final class FlexiCatBakedModel extends BakedModelWrapper<BakedModel> {

    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    public FlexiCatBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        CornerShape shape = data.get(FlexiCatModelProperties.SHAPE);
        if (shape == null || shape.isCube()) {
            return super.getQuads(state, side, rand, data, renderType);
        }
        TextureAtlasSprite sprite = getParticleIcon(data);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (ShapeMesh.Face face : ShapeMesh.build(shape)) {
            Direction cullFace = face.fullCubeFace() ? direction(face.face()) : null;
            if (cullFace != side) {
                continue;
            }
            quads.add(bake(face, sprite));
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        // Smooth lighting samples the 8 corners of the block cell; that is only
        // right for the placeholder cube.
        CornerShape shape = data.get(FlexiCatModelProperties.SHAPE);
        return shape == null || shape.isCube() ? TriState.DEFAULT : TriState.FALSE;
    }

    /** Pack one mesh face into vanilla's 8-int-per-vertex block format. */
    static BakedQuad bake(ShapeMesh.Face face, TextureAtlasSprite sprite) {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            ShapeMesh.Vertex v = face.vertex(i);
            int o = i * 8;
            vertices[o] = Float.floatToRawIntBits(v.x());
            vertices[o + 1] = Float.floatToRawIntBits(v.y());
            vertices[o + 2] = Float.floatToRawIntBits(v.z());
            vertices[o + 3] = -1; // white, untinted
            vertices[o + 4] = Float.floatToRawIntBits(sprite.getU(v.u() / (float) CornerShape.GRID));
            vertices[o + 5] = Float.floatToRawIntBits(sprite.getV(v.v() / (float) CornerShape.GRID));
            // [6], [7]: lightmap + normal, filled in by the renderer
        }
        return new BakedQuad(vertices, -1, direction(face.lightFace()), sprite, true, false);
    }

    static Direction direction(CubeFace face) {
        return switch (face) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }
}