# Infinite Generators

The `ra_infinite` module adds blocks that grow their own material in front of
themselves, forever. Point a [Block Breaker](interactive-machines.md) at one and
you have a self-feeding farm.

- Namespace: `ra_infinite`
- Give all: `/function ra_infinite:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Building one

A generator takes two halves: a **Generator Casing**, which is crafted, and a
**Core**, which is gambled for on an enchanting table.

1. Craft a Generator Casing — eight copper grates around one netherite scrap.
2. Throw stone, netherrack or poppies on an enchanting table. Each item has a
   **1%** chance of coming back as the matching Core; the rest are consumed. See
   [Enchant Crafting](enchant-crafting.md).
3. Put the Casing and the Core in a crafting grid.

| Core | Sacrifice | Generator it builds |
| ---- | --------- | ------------------- |
| Mineral Core | `minecraft:stone` | Mineral Generator |
| Nether Core | `minecraft:netherrack` | Nether Generator |
| Poppy Core | `minecraft:poppy` | Poppy Generator |

### Recipes

| Result | Recipe |
| ------ | ------ |
| Generator Casing | ![Generator Casing recipe](images/recipes/ra_infinite/generator_casing.png){ width="220" } |
| Mineral Generator | ![Mineral Generator recipe](images/recipes/ra_infinite/mineral_generator.png){ width="220" } |
| Nether Generator | ![Nether Generator recipe](images/recipes/ra_infinite/nether_generator.png){ width="220" } |
| Poppy Generator | ![Poppy Generator recipe](images/recipes/ra_infinite/poppy_generator.png){ width="220" } |

The three generator recipes are shapeless — the pictures put the casing and the
core side by side, but any two slots will do.

### Base items, and why they are odd

Vanilla recipe ingredients match by item id or tag and nothing else — components
are only available on a recipe's *result*. So the casing and the three cores each
sit on a different block item that no survival player can obtain or place:

| Item | Base | Looks like |
| ---- | ---- | ---------- |
| Generator Casing | `minecraft:repeating_command_block` | copper grate |
| Mineral Core | `minecraft:jigsaw` | deepslate diamond ore |
| Nether Core | `minecraft:structure_block` | ancient debris |
| Poppy Core | `minecraft:chain_command_block` | flowering azalea |

All four are `GameMasterBlockItem`s: placing one needs creative *and* permission
level 2, which is the same reason an Item Crate is a command block. Because the
ids differ, the three generator recipes tell each other apart on their own, a
bare netherite scrap cannot stand in for a casing, and no advancement guard or
intermediate item is needed. The Item Crate keeps the plain command block to
itself, so no recipe here can ever swallow a loaded crate.

## What they grow

| Generator | Period | Grows |
| --------- | ------ | ----- |
| Mineral Generator | 100 ticks | stone 57%, coal ore 20%, iron ore 14%, redstone ore 7.5%, diamond ore 1.2%, emerald ore 0.3% |
| Nether Generator | 100 ticks | netherrack 69%, magma block 30%, ancient debris 1% |
| Poppy Generator | 80 ticks | poppy, dandelion, cornflower, azure bluet, oxeye daisy, allium, blue orchid, lily of the valley — even odds |

At those rates a Mineral Generator averages a diamond every seven minutes or so
and an emerald every half hour, and a Nether Generator a piece of ancient debris
every ten minutes. Netherite blocks are deliberately not on the nether table —
four ingots out of one cycle would undo the material.

All three are real `minecraft:dropper` blocks, item and block alike, so the face
they grow from is obvious at a glance. They carry no block skin: a
`block_display` samples the light level at its own position, that position is
inside the opaque block it is covering, and the result renders black. Skins that
do still exist elsewhere in the pack now set `brightness` to work around it.

A dropper's redstone ejection is harmless here — a generator never stores
anything in its own inventory and never reads redstone.

## Behaviour

- A generator grows into the block **in front** of it, and only when that block
  is in `#ra_infinite:growable` — air, cave air, grass, ferns, dead bush or snow.
  Until the last one is taken away, nothing happens.
- The period is `data.properties.cooldown`, in ticks: 100 — five seconds — for the
  mineral and nether generators, 80 for the poppy generator. Edit it with the Data
  Handler.
- `data.properties.enabled` set to `0b` stops a generator without breaking it.
- Redstone is not involved at all — a generator runs whenever it is loaded.

### Where a Poppy Generator can plant

One flower in the block in front, per cycle. The 3×3 `patch` mode is gone — it was
a second code path over the same ground for a block whose whole job is one flower
at a time, and the Wrench no longer cycles anything on this block.

Flowers need something to stand on: `#ra_infinite:flower_ground` — grass block,
dirt, coarse dirt, rooted dirt, podzol, mycelium, moss, pale moss, mud, muddy
mangrove roots, farmland. Providing it is your job — the generator plants, it does
not terraform.

That tag lists block ids rather than borrowing `#minecraft:dirt`, and it has to.
**26.2 narrowed `#minecraft:dirt`** to dirt, coarse dirt and rooted dirt, moving
grass blocks into the new `#minecraft:substrate_overworld`. A tag that borrowed
`#minecraft:dirt` therefore excluded grass on 26.2 while still including it on
1.21.10 — the Poppy Generator refused to plant next to a grass block and said
nothing. Referencing `#minecraft:substrate_overworld` instead would break the other
end of the supported range, since that tag does not exist in 1.21.10.

It searches the 3×3 around the block in front and plants in the first spot that
takes a flower, checking three heights in each cell:

| Cell | Flower goes |
| ---- | ----------- |
| Empty, with soil one below | In that empty block |
| *Is* the soil — a grass block it stares at | On top of it |
| Empty, and the one below it empty too, with soil two below | One block down — a generator on a pedestal, planting on the ground it overlooks |

Insisting on the one block dead ahead is what made this look broken: a generator
facing slightly off, or with its soil beside rather than in front of it, planted
nothing and said nothing.

**Mind which way it faces.** A block placed while looking down faces *up*
(`dir_type:2`, pitch ≥ 45), and a Poppy Generator facing up targets the block
directly above itself, where the only thing under the target is the generator. It
will never plant there. Place it looking level so it faces sideways, either at a
grass block or across ground it can plant on.

The Goggles say which case you are in: a **Ground** line reading `ok` or `none`
tells you whether either of those two spots is plantable.

## Goggles

Wearing the Goggles shows each generator's period and enabled flag, plus the mode
and a **Ground** line on a Poppy Generator — `ok` when one of the nine cells it
searches can take a flower, `none` when none can.

`/function ra_infinite:debug/poppy` prints the same thing in more detail for every
Poppy Generator loaded: marker position and rotation, facing, cooldown, what the
block in front is, and the ground verdict.
