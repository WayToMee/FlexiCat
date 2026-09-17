# FlexiCat — design notes (v1)

This document records what the first version does, what it deliberately does not do, and the
decisions that are already fixed in code so later versions do not break saved worlds.

## The idea in one paragraph

A dedicated block (in the spirit of Create's copycats: filled with another block's material) plus
a tool. Right-clicking the block with the tool reveals its **eight corner points**. Selecting a
point and pressing movement keys moves that point along an axis on a **1/16 grid**. The block's
shape is the result of where the eight corners are. Lower one top corner by 8 and you have a
slanted top; lower two neighbouring top corners fully and you have a ramp.

## Fixed contracts (do not change casually)

These are baked into the geometry core (`common/…/geometry`) and into saved data.

1. **Corner identity.** Corners are indexed `0..7`; bit 0 = X side, bit 1 = Y side, bit 2 = Z side
   (`0` min, `1` max). A corner keeps its index wherever it is moved. Two corners at the same
   position are still two corners and can be separated again.
2. **Grid.** Positions are integers, 16 per block edge (`CornerShape.GRID`).
3. **Bounds (v1).** Every corner stays inside its own block cell, `[0, 16]` per axis. Whether
   corners may later leave the cell is open; the bound lives in one place (`CornerShape.MIN/MAX`).
   Out-of-range values read from disk are clamped, not rejected, so relaxing the bound later
   remains compatible in both directions.
4. **Face order.** Each face lists its four corners in a fixed counter-clockwise order (seen from
   outside the undeformed cube). Rendering, ray casting and culling depend on it.
5. **Triangulation.** A non-planar quad is split along the diagonal that passes through the
   corner lying **farthest from the plane of the other three** — equivalently, the corner whose
   opposite triangle has the smallest area (`FaceQuad.splitStart`). Ties (planar faces, symmetric
   saddles) use the first–third diagonal. Eight points alone do not define a curved surface; this
   rule makes the surface deterministic across saving, copying and mirroring, because it depends
   only on the positions. Consequence: lowering any single corner folds its faces *through* that
   corner, so the whole top reads as one slope. *Changed after in-game testing (stage 6 fix):
   the original rule was a fixed first–third diagonal, which folded through the moved corner
   for only two of a face's four corners — for the other two, the triangle formed by the three
   unmoved corners stayed flat and a crease ran across the middle of the block. Saved shapes are
   unaffected (only positions are stored); their surfaces re-triangulate on load.*
6. **Serialised form.** 24 bytes, corner-major, axis-minor. Loader codecs wrap this.

## What v1 is

- One block type, fillable with a material (like a copycat).
- One tool. Right-click a block → corners shown as handles. Right-click a handle to select it.
  Move the selected handle along an axis in ±1 grid steps.
- Server-authoritative shape: the client sends *intents* ("move corner 3 on Y by -1", payload
  `flexicat:corner_move`), the server validates (tool held, may build, block in reach and loaded,
  really a FlexiCat block, single grid step), applies the clamped move and syncs the block entity.
  The client does not predict; handles follow the synced shape.
- Selection is explicit: the handle under the crosshair is only *hovered* (highlighted white);
  a right-click with the tool on it makes it the selected corner (yellow), and it stays selected
  until another handle is clicked — the aim is free to wander while keys move the corner. (Stage 3
  used "sticky aim" — hover selected — which slipped too easily; changed after in-game testing.)
  Right-clicking the block itself (not a handle), letting go of the tool or walking out of reach
  ends editing.
- Move keys are read as *held state* each client tick, not as click events: one move on press,
  then after a short delay one move every two ticks while held (`HeldKeyRepeater`, pure Java).
  GLFW only auto-repeats the last key pressed, so the vanilla click counter stops while a walking
  key is also held; sampling the down state fixes that and caps the move rate (≈10/s) so a held
  key cannot flood the server with shape rebuilds. A move that cannot change the shape (corner
  already at the cell boundary) is not sent at all.
- Handles are picked against the *current* corner positions (`HandlePicker`), not the placeholder
  cube model, so they stay correct once the real mesh renders.
- Visual mesh, collision shape, ray-cast target, culling and neighbour behaviour are **separate
  systems** fed by the same `CornerShape`. They are allowed to disagree slightly (e.g. collision
  may be simplified) but must all derive from the corner data.

## Rendering (stage 4)

- `geometry/ShapeMesh` (common, pure) turns a `CornerShape` into faces: four vertices in block
  units plus UVs in 0–16 model units, in the face's fixed corner order (contract 4) rotated so
  that the emitted `v0–v2` is the split diagonal (contract 5). Minecraft's quad index buffer
  triangulates every quad as `0-1-2 / 0-2-3`, so the GPU draws exactly the contract triangles —
  the same ones the voxeliser (stage 5) uses for collision and picking. Degenerate faces
  (zero enclosed area, including two opposite corners coinciding) are skipped.
- Texture: the vanilla full-cube projection of each face, evaluated at the moved corner
  (`DOWN u=x,v=16-z · UP u=x,v=z · NORTH u=16-x,v=16-y · SOUTH u=x,v=16-y · WEST u=z,v=16-y ·
  EAST u=16-z,v=16-y`). A moved corner slides the texture with it; nothing is stretched over the
  whole face. Once material filling exists the same UVs sample the material's sprite.
- Culling: a face that is still the undeformed cube face (`FaceQuad.isFullCubeFace`) is emitted
  as a culled face for its direction, so vanilla drops it against opaque neighbours. Every other
  face is emitted unculled.
- Shading: each face carries a "light face" — the cube direction closest to its actual normal
  (ties favour Y) — used for directional shading and light sampling. Smooth lighting (AO) is
  disabled for non-cube shapes; it samples the cell corners and looks wrong on slopes.
- NeoForge wiring: `NeoForgeFlexiCatBlockEntity` publishes the shape as `ModelData` and requests a
  model-data refresh whenever a synced shape differs; `FlexiCatBakedModel` wraps the JSON
  placeholder model (kept for texture, particle sprite and item rendering) and builds quads at
  chunk-mesh time. The common block entity re-meshes the chunk section on sync
  (`onShapeSyncedOnClient`). Fabric will need the same three pieces with its own APIs.
- Outline and collision were the corners' bounding box until stage 5 (see below).

## Collision and picking (stage 5)

Minecraft's collision, block picking (ray cast) and support checks all consume a `VoxelShape`,
which can only be a union of axis-aligned boxes — there is no hook for exact triangle collision
without mixins. FlexiCat therefore approximates the deformed hull with boxes:

- `geometry/ShapeVoxelizer` (common, pure). The twelve contract triangles (contract 5) form a
  closed surface even for non-planar faces and concave shapes. Each of the 16³ cells inside the
  corner bounds is classified by its **centre** with a ray-parity test (odd number of triangle
  crossings = inside; the ray direction is deliberately irrational-ish so it never grazes an
  integer vertex or edge). Filled cells are merged greedily (x, then y, then z) into boxes. The
  result is deterministic, never fatter than the true shape and at most one cell thinner, so
  pulling a corner in never *adds* collision, and a slope voxelises into a monotone staircase.
- `block/FlexiCatShapes` (common) turns those boxes into a `VoxelShape` (`Shapes.or(...).optimize()`),
  with a full cube shortcut. A shape with no volume (flat plate, sliver) falls back to its bounding
  box padded to 1/16 so it can still be aimed at and stood on. Results are cached process-wide
  (LRU, keyed by `CornerShape`) on top of the per-block-entity cache, because many blocks share a
  shape.
- **Two resolutions.** The voxeliser takes a cell size. `getShape` (picking, outline) uses 1/16
  cells. `getCollisionShape` uses **4/16 cells** (`FlexiCatShapes.collisionOf`): vanilla's step-up
  logic climbs each box edge separately, so on a 1/16 staircase a player advances ~1/16 per tick
  and bobs sixteen times per block — slow and jerky. With 4/16 steps slopes are climbed at walking
  speed, like stairs, at the cost of the collision surface being off by up to one coarse cell.
  Picking is unaffected. A genuinely smooth slide would need an entity-movement mixin; not in v1.
- `FlexiCatBlock.getShape/getCollisionShape` return those shapes. The block **must** be registered
  with `dynamicShape()`: otherwise vanilla pre-computes the collision shape per block state at
  startup — with no block entity in reach — and every FlexiCat block would collide as a full cube
  (`isCollisionShapeFullBlock`, `largeCollisionShape`, `getCollisionShape(level,pos)` are all
  served from that cache).
- Picking: vanilla ray casts against the same voxel shape, so the aim lands on the sloped
  staircase (within one cell of the real surface) instead of the bounding box. The **outline**,
  however, would show that staircase; the loader's client module (`RenderHighlightEvent.Block` on
  NeoForge) cancels the vanilla outline for non-cube FlexiCat blocks and draws the twelve true
  edges (`CornerHandleRenderer.renderOutline`) in vanilla's outline colour. What is targeted is
  decided by the voxel shape; what is shown is the real geometry.
- Corner handles (`HandlePicker`) are unaffected: they are picked against the live corner
  positions, not against the voxel shape.
- Known limits: slopes are 4/16 staircases for entities (players "step" up ramps rather than
  slide — the same behaviour as stairs); flat plates collide as 1/16 slabs. Fabric needs its own
  outline hook and its own use-key hook for handle selection; the geometry and `VoxelShape`
  construction are shared.

## Material filling (stage 6)

A FlexiCat block starts as a placeholder-textured cube and can be *filled* with a material,
after which it looks like that block — the copycat idea.

- **Interaction.** Right-click an unfilled block with a block item → the block borrows that
  block's look and one item is consumed (not in creative). Sneak + right-click with an empty hand
  takes the material back out (returned to the inventory). Breaking the block drops the material
  alongside the block's own loot. The corner tool is unaffected: `useItemOn` passes anything that
  is not a fillable block item on to the default interaction, so the tool's `useOn` still runs.
- **What may fill it** (`block/FlexiCatMaterials`, common): a block that renders as a model, has
  no block entity, collides as a full cube and is not a FlexiCat block. Only the item's block's
  *default* state is stored — orientation is not kept (a log fills as an upright log). Stairs,
  slabs, fences, chests, invisible blocks and fluids are refused. Tinted blocks (grass, leaves)
  are fine.
- **Storage.** `FlexiCatBlockEntity` keeps a nullable `BlockState material`, saved under NBT key
  `Material` in `NbtUtils.writeBlockState` format. `setMaterial` is the write path (dirties the
  chunk, syncs to clients). A material whose block no longer exists reads back as empty. Shape
  and material are independent: filling does not change the shape, editing does not change the
  material.
- **Rendering (NeoForge).** The block entity publishes the material as `ModelData`
  (`FlexiCatModelProperties.MATERIAL`). `FlexiCatBakedModel` then:
  - for an *undeformed* filled block returns the material's own baked model quads unchanged, so
    multi-part vanilla models stay exact;
  - for a *deformed* filled block samples, per cube face, the material model's quad for that face
    (culled quads first, then unculled quads pointing that way) and applies its sprite and tint
    index to the FlexiCat mesh — whose UVs already follow the vanilla cube projection (stage 4),
    so the texture is placed exactly where it would be on the undeformed cube;
  - takes render layers, ambient occlusion (for cubes) and the particle sprite from the material.
  A block colour handler forwards tint queries to the material's colour handler, so biome
  colouring keeps working. Materials that only look right with a non-cube model or custom
  renderer are outside the acceptance rule above.
- **Sounds.** Place/remove play the material's own place/break sounds. The block's mining sound
  stays FlexiCat's for now (polish stage).

## Candidates for v1, not yet decided

- **Group movement**: move several selected corners with one keystroke. The geometry core already
  supports it (`moveGroup`, clamped as a whole so the group is not distorted).
- **Copy shape without replacing material**. (Materials exist since stage 6.)
- Move keys. Stage 3 ships **arrow keys** (up/down = world Y, left/right = relative to the
  player's facing) plus **Page Up / Page Down** (away / towards), all rebindable in the vanilla
  controls screen under "FlexiCat". WASD was avoided because it conflicts with walking around
  the block while editing. Whether this stays, or an axis-switch key is added, is open.

## Explicitly *not* in v1

- Adding vertices, cutting edges, extruding faces, subdivision — no topology changes.
- A separate editor window or preview-with-Save/Cancel flow. Editing happens in-world.
- Automatic welding to neighbours. (A one-time explicit "snap to neighbour" may come later.)
- Rotation/mirroring commands, a shape library, presets.

## Degenerate shapes

Users *will* collapse faces to lines and make zero-volume shapes. The core detects this
(`FaceQuad.isDegenerate`, `CornerShape.isFlat`) but does not forbid it: a fully flattened block is
a legitimate way to make a thin plate. Rendering and collision must tolerate degenerate faces
(skip them) rather than crash. Convexity is not enforced.

## Multi-loader / multi-version strategy

- `common` holds everything that only needs vanilla classes. It builds in NeoForm vanilla mode,
  so nothing loader-specific can compile there.
- A loader module (`neoforge`, later `fabric`) holds the entry point, registration glue,
  networking registration and rendering hooks.
- Version-specific code should be isolated behind small interfaces in `common` so a new Minecraft
  version means a new branch (or a version-suffixed loader module), not a rewrite.
- Branch naming for future versions: `1.21.1` is developed on `main` until a second version
  exists; then `main` tracks the newest and each older version gets its own branch.