# Isometric Renders - Forked

A NeoForge 1.21.1 fork/port of the original [Isometric Renders](https://github.com/glisco03/isometric-renders) mod.

This mod lets you create high-resolution PNG renders of Minecraft objects directly in-game. It is intended for documentation, modpack notes, wiki images, item catalogs, and visual reference exports.

## Features

- Render held or specified items
- Render item tooltips
- Render targeted or specified blocks
- Render block entities with NBT support
- Render targeted or specified entities
- Render entities with NBT support
- Select and render world areas/structures
- Export renders as PNG files
- Vanilla NeoForge GUI controls for:
  - Scale
  - Rotation
  - Slant
  - X/Y offset
  - Entity yaw/pitch
  - Export resolution
  - Coarse/fine adjustment mode

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Java 21

## Usage

All commands are client-side and begin with:

```mcfunction
/isorender
```

### Items

Render the item in your main hand:

```mcfunction
/isorender item
```

Render a specific item:

```mcfunction
/isorender item minecraft:diamond
```

### Tooltips

Render the tooltip for the item in your main hand:

```mcfunction
/isorender tooltip
```

Render the tooltip for a specific item:

```mcfunction
/isorender tooltip minecraft:diamond
```

### Blocks

Render the block you are looking at:

```mcfunction
/isorender block
```

Render a specific block state:

```mcfunction
/isorender block minecraft:oak_log[axis=y]
```

Render a block with NBT:

```mcfunction
/isorender block minecraft:chest[facing=north] {CustomName:'"Example Chest"'}
```

### Entities

Render the entity you are looking at:

```mcfunction
/isorender entity
```

Render a specific entity type:

```mcfunction
/isorender entity minecraft:zombie
```

Render an entity with NBT:

```mcfunction
/isorender entity minecraft:zombie {CustomName:'"Example Zombie"',NoAI:1b}
```

### Areas

Use the area selection key to select two corners of a region, then run:

```mcfunction
/isorender area
```

You can also render an explicit area:

```mcfunction
/isorender area 0 64 0 8 72 8
```

## Area Selection

Default keybind:

```text
C
```

Press once to select the first corner. Press again to select the second corner.

Sneak/Shift while pressing the selection key to clear the current selection.

## Exporting

After opening the render preview GUI, use the **Export** button to save a PNG.

Exports are written under the Minecraft game directory:

```text
renders/
```

## Build From Source

Clone the repository, then run:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The built jar will be generated under:

```text
build/libs/
```

## Notes

This fork does not currently include every feature from the original Fabric mod. In particular, batch rendering, item atlases, animation export, and the original owo-ui interface are not currently included.

The current implementation is focused on a native NeoForge 1.21.1 workflow with a lightweight vanilla GUI.

## Attribution

This project is a fork/port of the original **Isometric Renders** mod by glisco.

Original project:

```text
https://github.com/glisco03/isometric-renders
```

Port/fork modifications by OldManYounger.

This project is not affiliated with or endorsed by the original author unless otherwise stated.

## License

This project is licensed under the MIT License.

The original Isometric Renders project is also licensed under the MIT License. The original copyright and license notice are preserved in accordance with the MIT License.
