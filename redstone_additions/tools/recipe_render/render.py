#!/usr/bin/env python3
"""Draw a recipe on the real GUI it is made in.

Takes a data pack recipe file and writes a PNG the size and shape of the vanilla
screen for that recipe type — crafting table, furnace, blast furnace, smoker,
stonecutter or smithing table — scaled up whole-pixel.

    # any recipe file; output path is derived from its namespace and name
    python3 tools/recipe_render/render.py src/data/ra_infinite/recipe/mineral_generator.json

    # every recipe in the pack
    python3 tools/recipe_render/render.py --all

    # somewhere else, bigger
    python3 tools/recipe_render/render.py src/data/ra/recipe/wrench.json -o /tmp/wrench.png --scale 6

Assets come from the vanilla client jar and are cached per version under
`tools/recipe_render/assets/`; the first run downloads, later runs do not.

Items are read the way the game reads them: the recipe's item id goes through the
item definition and model chain to textures, and a `minecraft:item_model`
component on the result overrides the picture. That matters here, because every
Redstone Additions block is a bat spawn egg or an unobtainable command block
wearing a vanilla model. Ingredients cannot carry components, so the disguises
used *as* ingredients are listed in `overrides.json` instead.
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image  # noqa: E402

import atlas  # noqa: E402
from assets import ensure_assets  # noqa: E402
from models import Unrenderable, render_item  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
PACK_SRC = os.path.join(REPO, "redstone_additions", "src", "data")
DOCS_IMAGES = os.path.join(REPO, "docs", "images", "recipes")

# Vanilla screen geometry, in GUI pixels. Every container window is 176x166;
# cutting it off at 81 drops the player inventory and leaves the working area —
# the same crop and the same slot coordinates misode.github.io uses for its recipe
# previews (its SLOTS table lists slot *frames*, one pixel up and left of the item
# itself; the numbers below are the item positions, straight out of the menus).
WINDOW = (176, 166)
COMPACT_HEIGHT = 81
# The window's bottom edge — inner row plus the three border rows — is kept and
# pasted under the compact crop, so the panel closes instead of being sliced off.
BORDER_HEIGHT = 4
SLOT_PITCH = 18
SPRITE = 16
INVENTORY_LABEL_POS = (8, 72)
LABEL_COLOR = (63, 63, 63, 255)

# One entry per recipe screen: which GUI texture, what the window is called, where
# the title sits, where the result lands, and where the inputs go.
GUIS = {
    "crafting": {
        "texture": "crafting_table",
        "title": "Crafting",
        "title_pos": (28, 6),
        "result": (124, 35),
        "grid": (30, 17),
    },
    "furnace": {
        "texture": "furnace",
        "title": "Furnace",
        "title_pos": (8, 6),
        "result": (116, 35),
        "inputs": [(56, 17)],
        "fuel": (56, 53),
    },
    "blast_furnace": {
        "texture": "blast_furnace",
        "title": "Blast Furnace",
        "title_pos": (8, 6),
        "result": (116, 35),
        "inputs": [(56, 17)],
        "fuel": (56, 53),
    },
    "smoker": {
        "texture": "smoker",
        "title": "Smoker",
        "title_pos": (8, 6),
        "result": (116, 35),
        "inputs": [(56, 17)],
        "fuel": (56, 53),
    },
    "stonecutter": {
        "texture": "stonecutter",
        "title": "Stonecutter",
        "title_pos": (8, 6),
        "result": (143, 33),
        "inputs": [(20, 33)],
    },
    "smithing": {
        "texture": "smithing",
        "title": "Upgrade Gear",
        # The hammer is baked into the top left of smithing.png, so the label sits
        # beside it rather than at the usual 8, 6.
        "title_pos": (44, 15),
        "result": (98, 48),
        "inputs": [(8, 48), (26, 48), (44, 48)],
    },
}

# Recipe type -> screen. Campfire cooking has no GUI of its own, so it borrows the
# furnace, exactly as misode does.
RECIPE_GUIS = {
    "minecraft:crafting_shaped": "crafting",
    "minecraft:crafting_shapeless": "crafting",
    "minecraft:crafting_transmute": "crafting",
    "minecraft:smelting": "furnace",
    "minecraft:blasting": "blast_furnace",
    "minecraft:smoking": "smoker",
    "minecraft:campfire_cooking": "furnace",
    "minecraft:stonecutting": "stonecutter",
    "minecraft:smithing_transform": "smithing",
    "minecraft:smithing_trim": "smithing",
}


class Font:
    """The legacy ASCII sheet: 16x16 cells of 8x8 glyphs, indexed by char code.

    Glyph advance is measured off the sheet the same way the game does it — the
    last non-transparent column plus one, plus a pixel of spacing.
    """

    def __init__(self, path: str):
        self.sheet = Image.open(path).convert("RGBA")
        self.cell = self.sheet.width // 16
        self._widths: dict[str, int] = {}

    def _glyph(self, char: str) -> Image.Image:
        code = ord(char)
        col, row = code % 16, code // 16
        box = (col * self.cell, row * self.cell, (col + 1) * self.cell, (row + 1) * self.cell)
        return self.sheet.crop(box)

    def width(self, char: str) -> int:
        if char == " ":
            return 4
        if char not in self._widths:
            # Rightmost non-transparent column, plus a pixel of spacing.
            box = self._glyph(char).getchannel("A").getbbox()
            self._widths[char] = (box[2] + 1) if box else 4
        return self._widths[char]

    def text_width(self, text: str) -> int:
        return sum(self.width(char) for char in text)

    def draw(self, target: Image.Image, text: str, x: int, y: int, scale: int, color, shadow: bool) -> None:
        if shadow:
            dark = tuple(component // 4 for component in color[:3]) + (color[3],)
            self.draw(target, text, x + 1, y + 1, scale, dark, shadow=False)
        pen = x
        for char in text:
            glyph = self._glyph(char)
            tinted = Image.new("RGBA", glyph.size, color)
            tinted.putalpha(glyph.getchannel("A"))
            tinted = tinted.resize((glyph.width * scale, glyph.height * scale), Image.NEAREST)
            target.alpha_composite(tinted, (pen * scale, y * scale))
            pen += self.width(char)


def missing_texture(size: int) -> Image.Image:
    """The magenta-and-black square, for anything we cannot resolve."""
    image = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    half = size // 2
    for box in ((0, 0, half, half), (half, half, size, size)):
        image.paste((248, 0, 248, 255), box)
    return image


def ingredient_item(ingredient, root, overrides) -> tuple[str, str | None]:
    """Normalise one ingredient to (item id, item_model override or None).

    Recipes accept a bare id, a `#tag`, a list of ids, or the older
    `{"item": ...}` / `{"tag": ...}` objects. Only one thing can be drawn per
    slot, so a choice is drawn as its first option.
    """
    if isinstance(ingredient, dict):
        ingredient = ingredient.get("item") or ("#" + ingredient["tag"])
    if isinstance(ingredient, list):
        ingredient = ingredient[0]
    if ingredient.startswith("#"):
        ingredient = root.resolve_item_tag(ingredient[1:])[0]
    return ingredient, overrides.get(ingredient)


def shapeless_positions(count: int) -> list[tuple[int, int]]:
    """Where to put N ingredients that have no fixed shape."""
    if count <= 3:
        return [(1, col) for col in range(count)]
    return [(index // 3, index % 3) for index in range(min(count, 9))]


def recipe_slots(recipe: dict, gui: dict, root, overrides) -> list[tuple[tuple[int, int], tuple[str, str | None]]]:
    """[((gui x, gui y), (item id, item_model)), ...] for every input slot."""
    kind = recipe["type"]

    if "grid" in gui:
        origin = gui["grid"]
        placed: dict[tuple[int, int], tuple[str, str | None]] = {}

        if kind == "minecraft:crafting_shaped":
            pattern, key = recipe["pattern"], recipe["key"]
            # A pattern narrower or shorter than 3 sits at the top left, as in game.
            for row, line in enumerate(pattern):
                for col, symbol in enumerate(line):
                    if symbol != " ":
                        placed[(row, col)] = ingredient_item(key[symbol], root, overrides)
        else:
            if kind == "minecraft:crafting_transmute":
                ingredients = [recipe["input"], recipe["material"]]
            else:
                ingredients = recipe["ingredients"]
            for position, ingredient in zip(shapeless_positions(len(ingredients)), ingredients):
                placed[position] = ingredient_item(ingredient, root, overrides)

        return [
            ((origin[0] + col * SLOT_PITCH, origin[1] + row * SLOT_PITCH), item)
            for (row, col), item in placed.items()
        ]

    # The single-slot screens, and smithing's three, in the order the GUI shows.
    if kind in ("minecraft:smithing_transform", "minecraft:smithing_trim"):
        ingredients = [recipe.get("template"), recipe.get("base"), recipe.get("addition")]
    else:
        ingredients = [recipe["ingredient"]]

    return [
        (position, ingredient_item(ingredient, root, overrides))
        for position, ingredient in zip(gui["inputs"], ingredients)
        if ingredient is not None
    ]


def compose_window(background: Image.Image, scale: int, full_window: bool) -> Image.Image:
    """The GUI window, scaled, with a proper bottom edge.

    Cropping the 176x166 window at 81 rows drops the player inventory — the crop
    misode's previews use — but it also slices the panel open, which reads as a
    half-drawn image. So the last few rows of the real window, the inner row plus
    its border, are pasted back on underneath.
    """
    width, height = WINDOW
    if full_window:
        window = background.crop((0, 0, width, height))
    else:
        window = Image.new("RGBA", (width, COMPACT_HEIGHT), (0, 0, 0, 0))
        window.paste(background.crop((0, 0, width, COMPACT_HEIGHT - BORDER_HEIGHT)), (0, 0))
        window.paste(
            background.crop((0, height - BORDER_HEIGHT, width, height)),
            (0, COMPACT_HEIGHT - BORDER_HEIGHT),
        )
    return window.resize((window.width * scale, window.height * scale), Image.NEAREST)


def render_recipe(
    recipe: dict,
    root,
    overrides: dict,
    scale: int,
    labels: bool,
    full_window: bool,
    fuel: str | None = None,
) -> Image.Image:
    gui = GUIS[RECIPE_GUIS[recipe["type"]]]
    background = Image.open(root.gui_path(gui["texture"])).convert("RGBA")
    canvas = compose_window(background, scale, full_window)
    sprite = SPRITE * scale
    font = Font(root.font_path())

    def draw_sprite(item_id: str, item_model: str | None, gui_x: int, gui_y: int) -> None:
        try:
            icon = render_item(root, item_id, sprite, item_model)
        except Unrenderable as problem:
            print(f"  warning: {problem}", file=sys.stderr)
            icon = missing_texture(sprite)
        canvas.alpha_composite(icon, (gui_x * scale, gui_y * scale))

    for (gui_x, gui_y), (item_id, item_model) in recipe_slots(recipe, gui, root, overrides):
        draw_sprite(item_id, item_model, gui_x, gui_y)

    # A cooking recipe says nothing about fuel, so the slot is left empty unless
    # asked for.
    if fuel and "fuel" in gui:
        draw_sprite(fuel, overrides.get(fuel), *gui["fuel"])

    # smithing_trim has no result item — it writes a trim onto the base.
    result = recipe.get("result")
    if isinstance(result, str):
        result = {"id": result}
    if result and "id" in result:
        components = result.get("components", {})
        result_id = result["id"]
        draw_sprite(
            result_id,
            components.get("minecraft:item_model", overrides.get(result_id)),
            *gui["result"],
        )

        count = result.get("count", 1)
        if count > 1:
            text = str(count)
            font.draw(
                canvas,
                text,
                gui["result"][0] + 17 - font.text_width(text),
                gui["result"][1] + 9,
                scale,
                (255, 255, 255, 255),
                shadow=True,
            )

    if labels:
        font.draw(canvas, gui["title"], *gui["title_pos"], scale, LABEL_COLOR, shadow=False)
        if full_window:
            font.draw(canvas, "Inventory", *INVENTORY_LABEL_POS, scale, LABEL_COLOR, shadow=False)

    return canvas


def default_output(recipe_path: str, base_dir: str | None = None) -> str:
    """`src/data/<namespace>/recipe/<name>.json` -> `<base>/<namespace>/<name>.png`

    `base_dir` defaults to `docs/images/recipes`; point it somewhere else to look
    a batch over before it lands in the docs.
    """
    parts = os.path.normpath(os.path.abspath(recipe_path)).split(os.sep)
    try:
        namespace = parts[parts.index("recipe") - 1]
    except ValueError:
        namespace = "misc"
    name = os.path.splitext(os.path.basename(recipe_path))[0]
    return os.path.join(base_dir or DOCS_IMAGES, namespace, name + ".png")


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("recipes", nargs="*", help="recipe JSON files")
    parser.add_argument("--all", action="store_true", help="every recipe in src/data/*/recipe/")
    parser.add_argument("--only", help="with --all, only paths containing this")
    parser.add_argument("-o", "--output", help="output file (single recipe only)")
    parser.add_argument(
        "--out-dir",
        help="write <namespace>/<name>.png under this directory instead of docs/images/recipes",
    )
    parser.add_argument("--scale", type=int, default=4, help="GUI pixel scale, default 4 (704x324)")
    parser.add_argument(
        "--full-window",
        action="store_true",
        help="keep the whole 176x166 window, player inventory included",
    )
    parser.add_argument("--no-labels", action="store_true", help="omit the window title text")
    parser.add_argument(
        "--fuel",
        help="item to draw in a furnace fuel slot, e.g. minecraft:coal (empty by default)",
    )
    parser.add_argument("--mc-version", default="latest", help="'latest', 'snapshot' or an id")
    parser.add_argument("--assets", default=os.path.join(HERE, "assets"), help="asset cache")
    parser.add_argument("--refresh-assets", action="store_true", help="re-download even if cached")
    parser.add_argument(
        "--atlas",
        nargs="?",
        const="recipe-atlas.md",
        help="also write the recipe atlas page (default docs/recipe-atlas.md); on its own it only writes the page",
    )
    args = parser.parse_args()

    if args.atlas and not args.recipes and not args.all:
        rows = atlas.collect(PACK_SRC, DOCS_IMAGES, os.path.join(REPO, "docs"))
        gambles = atlas.collect_enchant(PACK_SRC)
        written = atlas.write(rows, args.atlas, os.path.join(REPO, "docs"), gambles)
        print(f"wrote {os.path.relpath(written, REPO)} ({len(rows)} recipes, {len(gambles)} enchant-table drops)")
        return

    targets = list(args.recipes)
    if args.all:
        targets += sorted(glob.glob(os.path.join(PACK_SRC, "*", "recipe", "*.json")))
    if args.only:
        targets = [path for path in targets if args.only in path]
    if not targets:
        parser.error("give at least one recipe file, or --all")
    if args.output and len(targets) > 1:
        parser.error("-o takes a single recipe")

    root = ensure_assets(args.assets, args.mc_version, args.refresh_assets)
    overrides = {
        key: value
        for key, value in json.load(open(os.path.join(HERE, "overrides.json"))).items()
        if not key.startswith("_")
    }

    skipped = 0
    for path in targets:
        recipe = json.load(open(path))
        if recipe.get("type") not in RECIPE_GUIS:
            print(f"skipping {os.path.basename(path)}: no GUI for {recipe.get('type')}")
            skipped += 1
            continue
        print(f"rendering {os.path.relpath(path, REPO)}")
        image = render_recipe(
            recipe, root, overrides, args.scale, not args.no_labels, args.full_window, args.fuel
        )
        output = args.output or default_output(path, args.out_dir)
        os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)
        image.save(output)
        print(f"  -> {os.path.relpath(output, REPO)} ({image.width}x{image.height})")

    if skipped:
        print(f"{skipped} non-crafting recipe(s) skipped")

    if args.atlas:
        rows = atlas.collect(PACK_SRC, DOCS_IMAGES, os.path.join(REPO, "docs"))
        gambles = atlas.collect_enchant(PACK_SRC)
        written = atlas.write(rows, args.atlas, os.path.join(REPO, "docs"), gambles)
        print(f"wrote {os.path.relpath(written, REPO)} ({len(rows)} recipes, {len(gambles)} enchant-table drops)")


if __name__ == "__main__":
    main()
