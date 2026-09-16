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
5. **Triangulation.** A non-planar quad is split along the diagonal between the face's first and
   third corner. Eight points alone do not define a curved surface; this rule makes the surface
   deterministic across saving, copying and mirroring.
6. **Serialised form.** 24 bytes, corner-major, axis-minor. Loader codecs wrap this.

## What v1 is

- One block type, fillable with a material (like a copycat).
- One tool. Right-click a block → corners shown as handles. Aim at a handle to select it.
  Move the selected handle along an axis in ±1 grid steps.
- Server-authoritative shape: the client sends *intents* ("move corner 3 on Y by -1"), the server
  validates, applies and syncs the block entity.
- Visual mesh, collision shape, ray-cast target, culling and neighbour behaviour are **separate
  systems** fed by the same `CornerShape`. They are allowed to disagree slightly (e.g. collision
  may be simplified) but must all derive from the corner data.

## Candidates for v1, not yet decided

- **Group movement**: move several selected corners with one keystroke. The geometry core already
  supports it (`moveGroup`, clamped as a whole so the group is not distorted).
- **Copy shape without replacing material**.
- Axis switching key and whether the move keys are WASD or arrows (WASD conflicts with walking;
  the user's message listed both as examples, not as a final layout).

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