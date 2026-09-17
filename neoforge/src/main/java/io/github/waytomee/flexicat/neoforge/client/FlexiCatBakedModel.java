package io.github.waytomee.flexicat.neoforge.client;

import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.geometry.CubeFace;
import io.github.waytomee.flexicat.geometry.ShapeMesh;
import io.github.waytomee.flexicat.neoforge.FlexiCatModelProperties;
import net.minecraft.client.Minecraft;
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
 * and optional material {@link BlockState} delivered through {@link ModelData}.
 *
 * <p>Wraps the JSON-baked placeholder model: that model still supplies the
 * particle sprite, the texture and the item rendering while the block is unfilled,
 * and is used unchanged when no shape is available (e.g. no block entity in reach of
 * the mesher).
 *
 * <p>With a material (stage 6) the look comes from the material's own baked model:
 * an undeformed filled block simply renders that model, so multi-part vanilla
 * models stay exact; a deformed one samples, per cube face, the sprite and tint of
 * the material's quad for that face and applies them to the FlexiCat mesh, whose
 * UVs already follow the vanilla cube projection (stage 4). Render layer, ambient
 * occlusion and particles are the material's.
 *
 * <p>Full, undeformed faces are returned for their {@link Direction} so vanilla
 * culls them against opaque neighbours exactly like a cube. Every deformed face is
 * returned as an unculled quad ({@code side == null}) and tagged with the direction
 * closest to its real normal for directional shading.
 */
public final class FlexiCatBakedModel extends BakedModelWrapper<BakedModel> {

    private static final ChunkRenderTypeSet PLACEHOLDER_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    public FlexiCatBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        CornerShape shape = data.get(FlexiCatModelProperties.SHAPE);
        BlockState material = data.get(FlexiCatModelProperties.MATERIAL);
        if (shape == null) {
            return super.getQuads(state, side, rand, data, renderType);
        }
        if (shape.isCube()) {
            if (material == null) {
                return super.getQuads(state, side, rand, data, renderType);
            }
            return modelOf(material).getQuads(material, side, rand, ModelData.EMPTY, renderType);
        }
        BakedModel materialModel = material == null ? null : modelOf(material);
        TextureAtlasSprite fallback = materialModel == null
                ? super.getParticleIcon(data) : materialModel.getParticleIcon(ModelData.EMPTY);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (ShapeMesh.Face face : ShapeMesh.build(shape)) {
            Direction faceDir = direction(face.face());
            Direction cullFace = face.fullCubeFace() ? faceDir : null;
            if (cullFace != side) {
                continue;
            }
            TextureAtlasSprite sprite = fallback;
            int tint = -1;
            if (materialModel != null) {
                BakedQuad source = sampleQuad(materialModel, material, faceDir, rand, renderType);
                if (source != null) {
                    sprite = source.getSprite();
                    tint = source.getTintIndex();
                }
            }
            quads.add(bake(face, sprite, tint));
        }
        return quads;
    }

    /** The material's quad for {@code faceDir}: culled quads first, then unculled ones pointing that way. */
    @Nullable
    private static BakedQuad sampleQuad(BakedModel model, BlockState material, Direction faceDir,
                                        RandomSource rand, @Nullable RenderType renderType) {
        List<BakedQuad> culled = model.getQuads(material, faceDir, rand, ModelData.EMPTY, renderType);
        if (!culled.isEmpty()) {
            return culled.getFirst();
        }
        for (BakedQuad quad : model.getQuads(material, null, rand, ModelData.EMPTY, renderType)) {
            if (quad.getDirection() == faceDir) {
                return quad;
            }
        }
        return null;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState material = data.get(FlexiCatModelProperties.MATERIAL);
        if (material == null) {
            return PLACEHOLDER_RENDER_TYPES;
        }
        return modelOf(material).getRenderTypes(material, rand, ModelData.EMPTY);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        // Smooth lighting samples the 8 corners of the block cell; that is only
        // right for an undeformed cube.
        CornerShape shape = data.get(FlexiCatModelProperties.SHAPE);
        if (shape != null && !shape.isCube()) {
            return TriState.FALSE;
        }
        BlockState material = data.get(FlexiCatModelProperties.MATERIAL);
        return material == null ? TriState.DEFAULT : modelOf(material).useAmbientOcclusion(material, ModelData.EMPTY, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState material = data.get(FlexiCatModelProperties.MATERIAL);
        return material == null ? super.getParticleIcon(data) : modelOf(material).getParticleIcon(ModelData.EMPTY);
    }

    private static BakedModel modelOf(BlockState material) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(material);
    }

    /** Pack one mesh face into vanilla's 8-int-per-vertex block format. */
    static BakedQuad bake(ShapeMesh.Face face, TextureAtlasSprite sprite, int tintIndex) {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            ShapeMesh.Vertex v = face.vertex(i);
            int o = i * 8;
            vertices[o] = Float.floatToRawIntBits(v.x());
            vertices[o + 1] = Float.floatToRawIntBits(v.y());
            vertices[o + 2] = Float.floatToRawIntBits(v.z());
            vertices[o + 3] = -1; // white; tinting (if any) is applied by the block colour handler
            vertices[o + 4] = Float.floatToRawIntBits(sprite.getU(v.u() / (float) CornerShape.GRID));
            vertices[o + 5] = Float.floatToRawIntBits(sprite.getV(v.v() / (float) CornerShape.GRID));
            // [6], [7]: lightmap + normal, filled in by the renderer
        }
        return new BakedQuad(vertices, tintIndex, direction(face.lightFace()), sprite, true, false);
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