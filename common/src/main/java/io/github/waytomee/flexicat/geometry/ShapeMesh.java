package io.github.waytomee.flexicat.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link CornerShape} into render-ready faces: four vertices per face in
 * block units (0.0–1.0) with texture coordinates in vanilla model units (0–16).
 *
 * <p>This is pure geometry, shared by every loader. The loader's client code only
 * has to pack these vertices into its baked-quad format.
 *
 * <p>Rules:
 * <ul>
 *   <li>Vertices are emitted in the face's fixed corner order ({@link CubeFace#corner}),
 *       rotated to start at {@link FaceQuad#splitStart()}, so {@code v0–v2} is the
 *       diagonal the surface folds along. Minecraft's quad index buffer triangulates
 *       every quad as {@code 0-1-2 / 0-2-3}, which reproduces the triangulation contract
 *       from {@code DESIGN.md} exactly — the same triangles the voxeliser uses.</li>
 *   <li>Degenerate faces (collapsed to a line or point) are skipped, never emitted.</li>
 *   <li>Texture coordinates use the vanilla cube projection of each face, so a
 *       moved corner slides the texture with it instead of stretching the whole face.</li>
 *   <li>Each face carries a {@link Face#lightFace()}: the cube face whose direction
 *       is closest to the deformed face's normal. Renderers use it for directional
 *       shading and light sampling.</li>
 * </ul>
 */
public final class ShapeMesh {

    /** One vertex: position in block units, texture coordinates in 0–16 model units. */
    public record Vertex(float x, float y, float z, float u, float v) {}

    /**
     * One renderable face.
     *
     * @param face          the cube face this quad descends from (defines its vertex order; the
     *                      order is rotated so {@code v0–v2} is the split diagonal)
     * @param lightFace     the face whose direction best matches the quad's actual normal
     * @param fullCubeFace  {@code true} if the quad is exactly the undeformed cube face and
     *                      may be culled against an opaque neighbour like a vanilla block face
     */
    public record Face(CubeFace face, CubeFace lightFace, boolean fullCubeFace, Vertex v0, Vertex v1, Vertex v2, Vertex v3) {

        public Vertex vertex(int i) {
            return switch (i & 3) {
                case 0 -> v0;
                case 1 -> v1;
                case 2 -> v2;
                default -> v3;
            };
        }
    }

    private ShapeMesh() {
    }

    /** All non-degenerate faces of the shape. */
    public static List<Face> build(CornerShape shape) {
        List<Face> faces = new ArrayList<>(6);
        for (CubeFace cubeFace : CubeFace.values()) {
            FaceQuad quad = shape.face(cubeFace);
            if (quad.isDegenerate()) {
                continue;
            }
            int start = quad.splitStart();
            Vertex[] vs = new Vertex[4];
            for (int i = 0; i < 4; i++) {
                Vec3i16 p = quad.vertex(start + i);
                float[] uv = uv(cubeFace, p);
                vs[i] = new Vertex((float) p.xBlocks(), (float) p.yBlocks(), (float) p.zBlocks(), uv[0], uv[1]);
            }
            faces.add(new Face(cubeFace, lightFace(quad), quad.isFullCubeFace(), vs[0], vs[1], vs[2], vs[3]));
        }
        return faces;
    }

    /**
     * Vanilla cube texture projection: the same mapping a full-cube JSON model uses
     * for each face, evaluated at an arbitrary grid position.
     */
    static float[] uv(CubeFace face, Vec3i16 p) {
        float x = p.x();
        float y = p.y();
        float z = p.z();
        float max = CornerShape.MAX;
        return switch (face) {
            case DOWN -> new float[] {x, max - z};
            case UP -> new float[] {x, z};
            case NORTH -> new float[] {max - x, max - y};
            case SOUTH -> new float[] {x, max - y};
            case WEST -> new float[] {z, max - y};
            case EAST -> new float[] {max - z, max - y};
        };
    }

    /**
     * The cube face whose outward direction is closest to the quad's area-weighted
     * normal. Falls back to the quad's own face when the normal is ambiguous.
     */
    static CubeFace lightFace(FaceQuad quad) {
        Vec3i16 n = quad.areaNormal();
        int ax = Math.abs(n.x());
        int ay = Math.abs(n.y());
        int az = Math.abs(n.z());
        if (ax == 0 && ay == 0 && az == 0) {
            return quad.face();
        }
        if (ay >= ax && ay >= az) {
            return CubeFace.of(Axis.Y, n.y() > 0);
        }
        if (ax >= az) {
            return CubeFace.of(Axis.X, n.x() > 0);
        }
        return CubeFace.of(Axis.Z, n.z() > 0);
    }
}