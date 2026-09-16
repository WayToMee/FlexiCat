# FlexiCat

A Minecraft mod adding a fillable block whose **corner points can be moved in-world with a tool**.

Take a block, right-click it with the FlexiCat tool: its eight corners light up. Pick one, push it
down half a block. Pick another, do the same — you have a ramp. There are no preset shapes; the
shape is whatever the corners say it is.

> The first version works with the eight corners of one block. Later versions are expected to go
> beyond that. FlexiCat borrows *a few* interaction ideas from Blender — it is not a mesh editor.

## Status

Early development. Nothing is playable yet; see [`docs/DESIGN.md`](docs/DESIGN.md) for the plan
and [`docs/ROADMAP.md`](docs/ROADMAP.md) for what comes next.

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