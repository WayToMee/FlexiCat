# FlexiCat

A Minecraft mod adding a fillable block whose **corner points can be moved in-world with a tool**.

Take a block, right-click it with the FlexiCat tool: its eight corners light up. Pick one, push it
down half a block. Pick another, do the same — you have a ramp. There are no preset shapes; the
shape is whatever the corners say it is.

> The first version works with the eight corners of one block. Later versions are expected to go
> beyond that. FlexiCat borrows *a few* interaction ideas from Blender — it is not a mesh editor.

## How to use it

Craft a **FlexiCat block** (four sticks around an iron nugget → 4 blocks) and a **corner tool**
(an iron ingot on a diagonal of two sticks), or take both from the creative menu.

1. **Place** the block and **right-click** it with the tool. Its twelve edges and eight corner
   points appear.
2. **Right-click a point** to select it (yellow). **Ctrl + right-click** adds more points to the
   group — select the four top points to lower a whole face at once.
3. **Move** the selection with the keys below. Hold a key to keep moving; corners stay on a 1/16
   grid inside the block's own cell.
4. **Fill** the block: right-click it with any plain full block (stone, planks, …) and it takes
   that block's look. **Sneak + right-click** with an empty hand takes the material back.
5. **Right-click the block** with the tool again to stop editing. **Sneak + right-click** with
   the tool resets the shape to a cube.

Collision, the outline and the crosshair target follow the real shape, so a lowered corner is a
slope you can walk up.

### Keys

All keys are rebindable under *Options → Controls → FlexiCat*. They only do anything while the
corner tool is held.

| Key | Action |
|-----|--------|
| `↑` / `↓` | move the selected corner(s) up / down |
| `←` / `→` | move left / right (relative to where you face) |
| `Page Up` / `Page Down` | move away from you / towards you |
| `Home` | copy the block's shape onto the tool |
| `End` | paste the tool's shape onto the block (material stays) |
| `Insert` | mirror the shape left ↔ right; with `Ctrl`: top ↔ bottom |
| `Delete` | rotate the shape 90° clockwise; with `Ctrl`: counter-clockwise |
| `Backspace` | undo the block's last change; with `Ctrl`: redo |

Copy, paste, mirror, rotate and undo work on the block being edited or, when not editing, on the
FlexiCat block under the crosshair. A held-key drag counts as one undo step.

Repeat speed, point size, sounds and the HUD hint are configurable in *Mods → FlexiCat → Config*
(`config/flexicat-client.toml`).

## Status

Early development, playable on NeoForge 1.21.1: editing, rendering, collision, material filling,
group moves, shape copy/paste, mirror/rotate, undo/redo, recipes and a client config are in. See
[`docs/DESIGN.md`](docs/DESIGN.md) for how it works and [`docs/ROADMAP.md`](docs/ROADMAP.md) for
what comes next.

| Target | Status |
|--------|--------|
| Minecraft 1.21.1 · NeoForge | in progress |
| Minecraft 1.21.1 · Fabric | planned |
| newer Minecraft versions | planned |

## Repository layout

```
common/    loader-agnostic code and assets (geometry core, block logic)
neoforge/  NeoForge entry point and loader glue
buildSrc/  shared Gradle conventions (flexicat-common, flexicat-loader)
docs/      design notes
```

`common` is compiled against plain Minecraft (NeoForm "vanilla mode"), so no loader-specific
class can leak into it. Each loader module compiles the `common` sources into its own jar, so a
release is one jar per loader.

## Building

Requires Java 21.

```
./gradlew build            # builds every module, runs tests
./gradlew :common:test     # geometry unit tests only (plain JVM, no Minecraft bootstrap)
./gradlew :neoforge:runClient
./gradlew :neoforge:runData
```

Jars land in `neoforge/build/libs/`.

## License

Not chosen yet — until then the project is all rights reserved by the author.