# Interactive Machines

The `ra_interactive` module provides 10 utility machines for automation and map logic.

- Namespace: `ra_interactive`
- Give all: `/function ra_interactive:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Summary

| Block | Item model | Recipe | Trigger model | Notes |
|---|---|---|---|---|
| Block Breaker | `minecraft:dispenser` | ![Block Breaker recipe](images/recipes/ra_interactive/block_breaker.png){ width="220" } | While powered | 40 tick action cooldown |
| Block Placer | `minecraft:dispenser` | ![Block Placer recipe](images/recipes/ra_interactive/block_placer.png){ width="220" } | While powered | Places from inventory into air in front |
| Item Pipe | `minecraft:dispenser` | ![Item Pipe recipe](images/recipes/ra_interactive/item_pipe.png){ width="220" } | Continuous | Moves whole stacks; filter via item frame |
| Item Mover | `minecraft:observer` | ![Item Mover recipe](images/recipes/ra_interactive/item_mover.png){ width="220" } | Continuous | Rear container to front container |
| Spitter | `minecraft:dropper` | ![Spitter recipe](images/recipes/ra_interactive/spitter.png){ width="220" } | Continuous | Throws item entities forward |
| Breeder | `minecraft:barrel` (dispenser skin) | ![Breeder recipe](images/recipes/ra_interactive/breeder.png){ width="220" } | While powered | Feeds animals from its own inventory |
| Infinite Water Cauldron | `minecraft:cauldron` | ![Infinite Water Cauldron recipe](images/recipes/ra_interactive/infinite_water_cauldron.png){ width="220" } | Continuous | Keeps `water_cauldron[level=3]` |
| Infinite Lava Cauldron | `minecraft:cauldron` | ![Infinite Lava Cauldron recipe](images/recipes/ra_interactive/infinite_lava_cauldron.png){ width="220" } | Continuous | Keeps `lava_cauldron` |
| Infinite Snow Cauldron | `minecraft:cauldron` | ![Infinite Snow Cauldron recipe](images/recipes/ra_interactive/infinite_snow_cauldron.png){ width="220" } | Continuous | Keeps `powder_snow_cauldron[level=3]` |
| Message Block | `minecraft:note_block` | ![Message Block recipe](images/recipes/ra_interactive/message_block.png){ width="220" } | Rising edge | Sends text to players in range |

## Behavior Notes

### Block Breaker

- Uses dispenser-facing direction.
- Breaks target block in front.
- Uses `ra.cooldown` with a 40 tick threshold.

### Block Placer

- Uses dispenser-facing direction.
- Attempts to place a valid block from its own inventory.

### Item Pipe

- Runs continuously without requiring redstone pulses.
- Moves a **whole stack** at a time when the destination has a free slot, falling
  back to one item at a time to top up partial stacks.
- A **filter** is an item frame attached to any face of the pipe. Matching items
  are diverted to an adjacent container instead of going forward. The frame is
  matched by the block it is attached to, so a frame on a neighbouring block is
  never mistaken for this pipe's filter.
- If the destination is full the item stays in the pipe. Pipes apply back
  pressure rather than dropping their contents.
- Moves items at a 4 tick cycle.
- Intentionally has no recipe in current release path.

### Item Mover

- Uses observer as the visual model.
- Moves one item from back inventory to front inventory each cycle.

### Spitter

- Uses dropper as visual/base block.
- Emits inventory items as entities from its facing side.

### Breeder

Put food in it, power it, and it feeds whatever is standing in front of it.

- The block is a **barrel wearing a dispenser skin**. It used to be a real
  dispenser, which throws its own inventory on any rising redstone edge — so a
  breeder loaded with wheat scattered the wheat across the field the moment you
  powered it. A barrel is the same inventory with none of that behaviour.
- Powered by redstone directly, and it looks **one block in front** of itself for
  an adult animal that is not already in love mode.
- One item is consumed per pair bred, out of its own inventory.

#### What it will breed, and on what

Two animals of the same kind must be in front of it, both adult, and the right
food must be somewhere in the barrel.

| Animal | Feed it |
|---|---|
| Cow | `wheat` |
| Mooshroom | `wheat` |
| Sheep | `wheat` |
| Pig | `carrot`, `potato`, `beetroot` |
| Chicken | `wheat_seeds`, `melon_seeds`, `pumpkin_seeds`, `beetroot_seeds`, `torchflower_seeds`, `pitcher_pod` |
| Goat | `wheat` |
| Rabbit | `carrot`, `golden_carrot`, `dandelion` |
| Horse | `golden_apple`, `golden_carrot`, `enchanted_golden_apple` |
| Llama | `hay_block` |
| Turtle | `seagrass` |
| Panda | `bamboo` |
| Fox | `sweet_berries`, `glow_berries` |
| Bee | any flower |
| Wolf | `beef`, `chicken`, `cooked_beef`, `cooked_chicken`, `cooked_mutton`, `cooked_porkchop`, `cooked_rabbit`, `mutton`, `porkchop`, `rabbit`, `rotten_flesh` |
| Cat | `cod`, `salmon` |
| Axolotl | `tropical_fish_bucket` |
| Strider | `warped_fungus` |
| Hoglin | `crimson_fungus` |
| Camel | `cactus` |
| Sniffer | `torchflower_seeds` |
| Frog | `slime_ball` |
| Armadillo | `spider_eye` |

Bees take any flower. Wolves take any raw or cooked meat, rotten flesh included.
Chickens take any of the six seeds, and sniffers share torchflower seeds with
them — a breeder holding torchflower seeds in front of both will feed whichever
it finds first.

### Infinite Cauldrons

- Water and snow versions enforce level 3.
- Lava version enforces lava state.
- Designed to be self-healing utility sources.

### Message Block

- Internal ID uses `message_block` (placement tag and custom_data).
- Folder path remains `blocks/message`.
- Default properties initialized to message text and range.

## Contributor Notes

1. Keep machine-facing behavior tied to `dir_type` and orientation storage.
2. For inventory machines, prefer `ra_lib:inventory/insert` and `ra_lib:inventory/remove` helpers.
3. For redstone-triggered behavior, keep edge detection (`ra.was_powered`) explicit.

---
