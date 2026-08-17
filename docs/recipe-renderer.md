# Recipe Renderer

`tools/recipe_render/` draws a recipe file onto the vanilla GUI it is made in and
saves it as a PNG for these pages. Recipe pictures used to be a screenshot each,
which meant opening the game for every new item.

Needs Pillow (`pip install pillow`) and, on the first run only, network access.

## Quick commands

Run from `redstone_additions/`.

```bash
# one recipe -> docs/images/recipes/<namespace>/<name>.png
python3 tools/recipe_render/render.py src/data/ra_infinite/recipe/mineral_generator.json

# every crafting recipe in the pack
python3 tools/recipe_render/render.py --all

# one namespace at a time
python3 tools/recipe_render/render.py --all --only ra_jetpacks

# somewhere else, bigger
python3 tools/recipe_render/render.py src/data/ra/recipe/wrench.json -o /tmp/wrench.png --scale 6

# regenerate the Recipe Atlas page from the recipe files
python3 tools/recipe_render/render.py --atlas

# both: redraw every picture and rebuild the atlas around them
python3 tools/recipe_render/render.py --all --atlas

# pin the Minecraft version, or force a re-download
python3 tools/recipe_render/render.py --all --mc-version 26.2
python3 tools/recipe_render/render.py --all --refresh-assets
```

| Flag | Meaning |
| ---- | ------- |
| `--all` | every `src/data/*/recipe/*.json` |
| `--only <text>` | keep only paths containing `<text>` |
| `-o <file>` | output path, single recipe only |
| `--out-dir <dir>` | write `<namespace>/<name>.png` here instead of into the docs |
| `--scale <n>` | GUI pixel scale, default `4` → 704x324 |
| `--full-window` | keep all 176x166, player inventory included |
| `--no-labels` | drop the window title text |
| `--fuel <item>` | what to draw in a furnace fuel slot (empty by default) |
| `--mc-version <v>` | `latest` (default), `snapshot`, or an id like `26.2` |
| `--refresh-assets` | re-download even if the version is cached |
| `--atlas [file]` | write the [Recipe Atlas](recipe-atlas.md) page, default `docs/recipe-atlas.md`. On its own it writes only the page |

## Assets

The first run resolves the version through Mojang's manifest, downloads that
client jar, keeps what a recipe picture needs — item and block textures, the
item/model definitions, the crafting GUI, the ASCII font, the vanilla item tags —
and deletes the jar. Later runs reuse `tools/recipe_render/assets/<version>/`,
which is git-ignored. A pinned version that is already cached needs no network.

## Recipe types

| Recipe type | GUI | Slots drawn |
| ----------- | --- | ----------- |
| `crafting_shaped`, `crafting_shapeless`, `crafting_transmute` | crafting table | 3×3 grid at `30 + col*18, 17 + row*18`, result `124, 35` |
| `smelting` | furnace | input `56, 17`, fuel `56, 53`, result `116, 35` |
| `blasting` | blast furnace | as smelting |
| `smoking` | smoker | as smelting |
| `campfire_cooking` | furnace | as smelting — campfires have no screen of their own |
| `stonecutting` | stonecutter | input `20, 33`, result `143, 33` |
| `smithing_transform`, `smithing_trim` | smithing table | template `8, 48`, base `26, 48`, addition `44, 48`, result `98, 48` |

Anything else — `crafting_special_*`, a custom type from another pack — is skipped
with a message rather than drawn on the wrong background.

Two GUIs look sparse on purpose. A cooking recipe says nothing about fuel, so that
slot is empty unless `--fuel minecraft:coal` is passed, and the stonecutter's dark
recipe-list panel is part of the vanilla texture — in game it is covered by recipe
buttons.

## Output

176x81 of the window at scale 4, so **704x324**: the slots, the arrow and the
result, with the player inventory cropped off. That is the crop
[misode.github.io](https://misode.github.io) uses for its recipe previews, and its
`SLOTS` table agrees with the coordinates above — theirs name the slot frame, one
pixel up and left of the item.

Cutting the window at 81 rows also cuts its bottom border off, which reads as a
half-drawn image, so the window's last four rows — the inner row and its border —
are pasted back on underneath. `--full-window` keeps all 176x166 instead, player
inventory and all. Reference a picture at the usual width:

```markdown
![Mineral Generator recipe](images/recipes/ra_infinite/mineral_generator.png){ width="220" }
```

## How an item becomes a picture

The same three hops the game makes:

```
assets/minecraft/items/<id>.json      which model
assets/minecraft/models/<model>.json  the model and its parent chain
assets/minecraft/textures/...         the textures the chain names
```

A chain ending in `item/generated` is a flat sprite, layers composited in order.
Anything else is a block model and is rendered from its real elements, the way the
game and deepslate do it:

1. each element is a box from `from` to `to` in 0..16 model space, with its own
   optional rotation around an axis;
2. the model's `display.gui` transform is applied about the block centre —
   rotation `[30, 225, 0]`, scale `0.625` for anything inheriting `block/block`;
3. the result is projected orthographically, which keeps every box face a
   parallelogram, so its texture maps with a plain affine transform;
4. faces pointing away are culled, the rest drawn far to near;
5. each face is shaded by its facing with vanilla's constants — up `1.0`, down
   `0.5`, north/south `0.8`, east/west `0.6`.

So stairs look like stairs, a daylight detector is slab height, azalea keeps its
leaves, and a dropper's front lands on the same side the game puts it. Face uvs come
from the model where given and from `FaceBakery.defaultFaceUV` where not, and face
`rotation` is honoured. Items with no model at all (chests, banners, shields, heads)
render as the missing-texture square and print a warning.

Shapeless recipes are laid out for the picture only: up to three ingredients sit
in the middle row, more fill the grid from the top left. In game the position
never matters.

### Where this differs from misode's previews

Worth knowing before trusting a picture:

- **Ingredient choices.** A `#tag` or a list of items draws its first option.
  misode cycles through the options once a second.
- **No tints.** `tintindex` is ignored, so grass, leaves and water render in their
  greyscale texture colours rather than a biome tint.
- **No cook time, experience or trim pattern** is drawn — only the items.
- **Painter's algorithm.** Faces are sorted by depth rather than z-buffered, which
  is exact for box faces but could misorder a model whose elements interpenetrate.

Geometry, window crop, slot coordinates and item resolution now match: same display
transform, same face order, same shading constants. misode's runs on the GPU through
deepslate, this one rasterises with Pillow.

## The atlas page

`--atlas` writes [Recipe Atlas](recipe-atlas.md): every recipe in the pack on one
page, alphabetically and again grouped by module. Names come out of each recipe's
`minecraft:item_name`, the station from its type, and the picture from
`docs/images/recipes/`, so the page describes what the pack contains rather than what
someone remembered to add. A recipe with no rendered picture yet is still listed, with
a note in place of the image.

Adding a module means one line in `MODULES` in `tools/recipe_render/atlas.py`, which
maps a namespace to its title and doc page. A namespace missing from that map still
gets a section, titled after the namespace.

## Disguised ingredients

Every Redstone Additions block is a vanilla item wearing someone else's
`minecraft:item_model` — a bat spawn egg, or an unobtainable command block. On a
recipe **result** that component is in the JSON and the renderer uses it, which is
why the generators show up as droppers. On an **ingredient** there are no
components at all, because vanilla matches ingredients by id, so the disguise has
to be declared in `tools/recipe_render/overrides.json`:

```json
{
  "minecraft:repeating_command_block": "minecraft:copper_grate",
  "minecraft:jigsaw": "minecraft:deepslate_diamond_ore",
  "minecraft:structure_block": "minecraft:ancient_debris",
  "minecraft:chain_command_block": "minecraft:flowering_azalea"
}
```

Add an entry whenever a new RA item becomes an ingredient, and keep it in step
with the give function that hands that item out. `minecraft:command_block` is
deliberately absent — the Item Crate keeps that one and is never an ingredient.

## Adding a picture for a new recipe

1. Write `src/data/<namespace>/recipe/<name>.json`.
2. If it consumes a disguised RA item, add it to `overrides.json`.
3. `python3 tools/recipe_render/render.py src/data/<namespace>/recipe/<name>.json`
4. Reference the PNG from the module page.

Non-crafting recipe types — smelting, stonecutting, smithing — are skipped with a
message, since the crafting GUI would be the wrong background for them.
