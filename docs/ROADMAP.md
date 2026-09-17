# Roadmap

Small stages, each reviewable on its own.

- [x] **0.** Repository scaffold: Gradle multi-module (common + neoforge), builds a loadable jar.
- [x] **1.** Geometry core: `CornerShape`, corner/face model, clamped moves, group moves,
      planarity/degeneracy checks, byte serialisation, unit tests.
- [x] **2.** Block + block entity on NeoForge: registration, shape codec/NBT, client sync,
      placeholder full-cube rendering. Loads on a dev server; corner tool shows the shape summary.
- [x] **3.** Tool: right-click reveals corner handles; right-click a handle to select it (was
      aim-select; changed after in-game testing); server-validated move packets. Arrow keys /
      Page Up / Page Down move the selected corner (rebindable, held keys repeat); no client prediction.
- [x] **4.** Rendering: mesh built from the corner positions (planar-safe triangulation), vanilla
      cube texture projection per face, culling of untouched faces, directional shading by real
      normal. Outline/collision use the corners' bounding box for now.
- [x] **5.** Collision + ray casting derived from the actual faces: the enclosed volume is
      voxelised on the 1/16 grid into a `VoxelShape` (picking, support) and on a 4/16 grid for
      entity collision (walkable slopes), and the block outline shows the shape's true twelve
      edges instead of the voxel staircase.
- [x] **6.** Material filling (copycat behaviour): right-click with a plain full-cube block item
      to borrow its textures, tint, render layer and particles; sneak + empty hand takes it back;
      dropped on break. Deformed faces sample the material's face sprites through the stage-4 UVs.
- [ ] **7.** Polish: group movement, shape copy, sounds/particles, config, localisation.
- [ ] **F.** Fabric module.