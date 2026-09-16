# Roadmap

Small stages, each reviewable on its own.

- [x] **0.** Repository scaffold: Gradle multi-module (common + neoforge), builds a loadable jar.
- [x] **1.** Geometry core: `CornerShape`, corner/face model, clamped moves, group moves,
      planarity/degeneracy checks, byte serialisation, unit tests.
- [ ] **2.** Block + block entity on NeoForge: registration, shape codec/NBT, client sync,
      placeholder full-cube rendering. Loads in a dev client.
- [ ] **3.** Tool: right-click reveals corner handles; aim-select; server-validated move packets.
- [ ] **4.** Rendering: mesh built from the corner positions (planar-safe triangulation), material
      texture projection.
- [ ] **5.** Collision + ray casting derived from the shape; culling of full faces against
      neighbours.
- [ ] **6.** Material filling (copycat behaviour).
- [ ] **7.** Polish: group movement, shape copy, sounds/particles, config, localisation.
- [ ] **F.** Fabric module.