# Drill Multiblock Structure (rock_metallic)

## Side View
```
B M
C S
```
- **B** = Barrel (input, at ~1 ~ ~ relative to base)
- **M** = Main base (Copper Block, at base position)
- **C** = Chains (below barrel, at ~1 ~-1 ~)
- **S** = Smooth Stone (below base, at ~ ~-1 ~)

## Functionality
- Emits stone block particles and sound when active.
- Produces a custom item: **ROCK**
  - Command: `/give CaptainDragons minecraft:command_block[minecraft:item_name="Rock",minecraft:item_model=dead_brain_coral_block,minecraft:rarity=common]`
  - Output is inserted into the output barrel.
- (Future) ROCK will be broken into ROCK_GRAVELAT:
  - `/give CaptainDragons minecraft:command_block[minecraft:item_name="Rock",minecraft:item_model=dead_tube_coral_block,minecraft:rarity=common]`

## Tags & Functions
- Uses tag: `ra.multiblock.drill`
- Main functions: `check_blocks`, `process_facing`, `tick_facing`, `tick`, `tick_dispatch`, `setup_type`, `on_break`, `check_structure`

---
*This file documents the structure and output for the drill multiblock (rock_metallic variant).*
