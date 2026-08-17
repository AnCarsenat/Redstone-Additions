# recipe_render

Draws a data pack recipe on the vanilla GUI it is made in and saves it as a PNG for
the docs. Replaces taking a screenshot per recipe.

```bash
cd redstone_additions

# one recipe -> docs/images/recipes/<namespace>/<name>.png
python3 tools/recipe_render/render.py src/data/ra_infinite/recipe/mineral_generator.json

# every recipe in the pack
python3 tools/recipe_render/render.py --all

# a whole batch into a staging folder, to look over before it lands in the docs
python3 tools/recipe_render/render.py --all --out-dir tools/recipe_render/out

# rebuild the Recipe Atlas page (docs/recipe-atlas.md) from the recipe files
python3 tools/recipe_render/render.py --atlas
python3 tools/recipe_render/render.py --all --atlas   # pictures and page together

# a subset, somewhere else, bigger
python3 tools/recipe_render/render.py --all --only ra_jetpacks
python3 tools/recipe_render/render.py src/data/ra/recipe/wrench.json -o /tmp/wrench.png --scale 6
```

Needs Pillow and, on the first run, network access.

## Files

| File | Job |
| ---- | --- |
| `render.py` | CLI, GUI compositing, font, recipe layout |
| `assets.py` | version resolution, client jar download, extraction, cache |
| `models.py` | item id to picture: item definition, model chain, textures |
| `overrides.json` | which vanilla model to draw for a disguised RA ingredient |
| `atlas.py` | the Recipe Atlas page: module titles, give commands, page layout |
| `assets/<version>/` | extracted vanilla assets, git-ignored, re-downloaded on demand |

## Assets

The first run resolves `latest` through Mojang's version manifest, downloads that
client jar, keeps the item and block textures, the item/model definitions, the
crafting GUI, the ASCII font and the vanilla item tags, and deletes the jar. Later
runs reuse `assets/<version>/`.

```bash
python3 tools/recipe_render/render.py --all --mc-version 26.2      # pin a version
python3 tools/recipe_render/render.py --all --refresh-assets       # re-download
```

A pinned version that is already cached needs no network at all. `latest` always
asks the manifest what latest means.

## Recipe types

Crafting table (`crafting_shaped`, `crafting_shapeless`, `crafting_transmute`),
furnace (`smelting`), blast furnace (`blasting`), smoker (`smoking`), furnace again
for `campfire_cooking`, stonecutter (`stonecutting`) and smithing table
(`smithing_transform`, `smithing_trim`). Slot coordinates come from the vanilla
menus and agree with misode's `SLOTS` table. Anything else is skipped with a
message. Fuel slots are empty unless `--fuel minecraft:coal` is passed.

## Output

176x81 of the window — grid, arrow, result — at whole-pixel scale 4, so
704x324. That is the crop [misode.github.io](https://misode.github.io) uses for
its recipe previews, and the slot coordinates match the game's own
(`CraftingMenu`: grid items at `30 + col*18, 17 + row*18`, result at `124, 35`).
`--full-window` keeps all 176x166 including the empty player inventory,
`--no-labels` drops the window title, and `--out-dir` writes the batch somewhere
other than the docs so it can be looked over first.

## How an item becomes a picture

The same three hops the game uses:

```
assets/minecraft/items/<id>.json      which model
assets/minecraft/models/<model>.json  the model, and its parent chain
assets/minecraft/textures/...         the textures the chain names
```

A chain that ends in `item/generated` is a flat sprite. Anything else is a block
model, drawn as an isometric cube from its top, front and side textures — each
face is a parallelogram, so an affine transform of the texture is enough. Blocks
whose real model is not a cube (stairs, slabs, fences) come out as cubes, and an
ingredient choice (`#tag` or a list) draws its first option. Those are the two
places this differs from misode's previews, which run the real model through
WebGL — `display.gui` transform included — and cycle the options.

Branching item definitions (a condition on `broken`, a select on the date) take
the plainest branch. Items with no model at all — chests, banners, shields, heads
— render as the magenta-and-black missing texture and print a warning.

## Overrides

Every Redstone Additions block is a vanilla item wearing someone else's
`minecraft:item_model`: a bat spawn egg, or an unobtainable command block. On a
recipe **result** that component is right there in the JSON and gets used. On an
**ingredient** there are no components at all — vanilla matches ingredients by id
— so a recipe that eats a disguised item would otherwise draw the base item. Those
are listed in `overrides.json`:

```json
"minecraft:jigsaw": "minecraft:deepslate_diamond_ore"
```

Add an entry whenever a new RA item becomes an ingredient, and keep it in step
with the give function that hands the item out.

## Adding a recipe picture

1. Write the recipe under `src/data/<namespace>/recipe/<name>.json`.
2. If it consumes a disguised RA item, add it to `overrides.json`.
3. `python3 tools/recipe_render/render.py src/data/<namespace>/recipe/<name>.json`
4. Reference it from the module page:
   `![<name> recipe](images/recipes/<namespace>/<name>.png){ width="220" }`

Recipe types with no GUI of their own — `crafting_special_*`, anything custom — are
skipped with a message rather than drawn on the wrong background.
