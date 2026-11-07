"""Generate the recipe atlas page.

One page with every recipe in the pack, grouped by module and indexed by name, so a
player looking for something has a single place to look. Generated rather than
written, because a hand-kept list of 58 recipes drifts the moment one is added: the
names come out of the recipes themselves.
"""

import json
import os
import re

# Module title and doc page per namespace. A namespace missing from here still gets
# a section, titled after itself.
MODULES = {
    "ra": ("Tools", "index.md"),
    "ra_gates": ("Logic Gates", "logic-gates.md"),
    "ra_interactive": ("Interactive Machines", "interactive-machines.md"),
    "ra_storage": ("Storage", "storage.md"),
    "ra_sensors": ("Sensors", "sensors.md"),
    "ra_wireless": ("Wireless Redstone", "wireless-redstone.md"),
    "ra_wires": ("Transport Networks", "transport-networks.md"),
    "ra_chunk_loader": ("Chunk Loader", "chunk-loader.md"),
    "ra_multiblock": ("Multiblocks", "multiblocks.md"),
    "ra_infinite": ("Infinite Generators", "infinite-generators.md"),
    "ra_jetpacks": ("Jetpacks", "jetpacks.md"),
    "ra_ender": ("Ender Links", "ender-links.md"),
}

GIVE = {
    "ra": "/function ra:tools/give_all",
    "ra_gates": "/function ra_gates:items/give_all",
    "ra_interactive": "/function ra_interactive:items/give_all",
    "ra_storage": "/function ra_storage:items/give_all",
    "ra_sensors": "/function ra_sensors:items/give_all",
    "ra_wireless": "/function ra_wireless:items/give_all",
    "ra_wires": "/function ra_wires:items/give_all",
    "ra_chunk_loader": "/function ra_chunk_loader:items/give_all",
    "ra_multiblock": "/function ra_multiblock:blocks/give_all",
    "ra_infinite": "/function ra_infinite:items/give_all",
    "ra_jetpacks": "/function ra_jetpacks:items/give_all",
    "ra_ender": "/function ra_ender:items/give_all",
}

TYPE_NAMES = {
    "minecraft:crafting_shaped": "Crafting table",
    "minecraft:crafting_shapeless": "Crafting table, shapeless",
    "minecraft:crafting_transmute": "Crafting table",
    "minecraft:smelting": "Furnace",
    "minecraft:blasting": "Blast furnace",
    "minecraft:smoking": "Smoker",
    "minecraft:campfire_cooking": "Campfire",
    "minecraft:stonecutting": "Stonecutter",
    "minecraft:smithing_transform": "Smithing table",
    "minecraft:smithing_trim": "Smithing table",
}


def collect(pack_src, images_root, docs_root):
    """Every recipe as (namespace, name, display name, station, image path)."""
    out = []
    for namespace in sorted(os.listdir(pack_src)):
        folder = os.path.join(pack_src, namespace, "recipe")
        if not os.path.isdir(folder):
            continue
        for file in sorted(os.listdir(folder)):
            if not file.endswith(".json"):
                continue
            name = file[:-5]
            data = json.load(open(os.path.join(folder, file)))
            result = data.get("result")
            result = {"id": result} if isinstance(result, str) else (result or {})
            components = result.get("components") or {}
            display = components.get("minecraft:item_name") or result.get("id", name).split(":")[-1].replace("_", " ").title()
            image = os.path.join("images", "recipes", namespace, name + ".png")
            if not os.path.isfile(os.path.join(docs_root, image)):
                image = None
            out.append((namespace, name, display, TYPE_NAMES.get(data.get("type"), "Special"), image))
    return out


def collect_enchant(pack_src):
    """Items won on an enchanting table, read out of the enchant_recipes functions.

    Each recipe there is one `if data storage ra:enchant input{...}` line that writes
    a result, then a `chance set value N` line, and by convention a `# Sacrifice ->
    Result` comment above it. The sacrifice is taken from the comment because the
    input can be matched on custom_data — "an iron jetpack kit" is not something to
    reconstruct from `{ra:{jetpack_kit:1b,tier:"iron"}}`.
    """
    out = []
    for namespace in sorted(os.listdir(pack_src)):
        path = os.path.join(pack_src, namespace, "function", "enchant_recipes.mcfunction")
        if not os.path.isfile(path):
            continue
        lines = open(path).read().split("\n")
        for index, line in enumerate(lines):
            if "ra:enchant input{" not in line:
                continue
            name = re.search(r'"minecraft:item_name":"([^"]+)"', line)
            if not name:
                continue

            # The sacrifice: the comment above if it names a pair, else the item id.
            sacrifice = None
            for back in range(index - 1, max(index - 4, -1), -1):
                comment = lines[back].strip()
                if not comment.startswith("#"):
                    break
                if "\u2192" in comment or "->" in comment:
                    sacrifice = re.split(r"\u2192|->", comment.lstrip("# "))[0].strip()
                    break
            if not sacrifice:
                plain = re.search(r'input\{id:"([^"]+)"\}', line)
                sacrifice = plain.group(1) if plain else "a specific item"

            chance = None
            for ahead in lines[index + 1:index + 4]:
                found = re.search(r"chance set value (\d+)", ahead)
                if found:
                    chance = found.group(1)
                    break

            out.append((namespace, sacrifice, name.group(1), chance or "?"))
    return out


def write(rows, target, docs_root, enchant=()):
    lines = [
        "# Recipe Atlas",
        "",
        "Every recipe in the pack, in one place: " + str(len(rows)) + " of them.",
        "",
        "!!! tip \"Finding one\"",
        "    Use the search box with the item's name — every recipe below is indexed by",
        "    it. The table right underneath is the same list alphabetically, and each",
        "    module section links to the page explaining what those blocks do.",
        "",
        "This page is generated by `tools/recipe_render/render.py --atlas`, so it lists",
        "what the pack actually contains rather than what someone remembered to add.",
        "",
        "## Every item, A to Z",
        "",
        "| Item | Module | Station |",
        "| ---- | ------ | ------- |",
    ]

    index = [(r[0], r[2], r[3], None) for r in rows]
    index += [(n, result, "Enchanting table", "#not-crafted-won-on-an-enchanting-table")
              for n, _sacrifice, result, _chance in enchant]

    for namespace, display, station, override in sorted(index, key=lambda r: r[1].lower()):
        title, page = MODULES.get(namespace, (namespace, None))
        anchor = override or ("#" + title.lower().replace(" ", "-"))
        lines.append("| [%s](%s) | [%s](%s) | %s |" % (display, anchor, title, page or anchor, station))

    by_module = {}
    for row in rows:
        by_module.setdefault(row[0], []).append(row)

    for namespace in sorted(by_module, key=lambda n: MODULES.get(n, (n,))[0]):
        title, page = MODULES.get(namespace, (namespace, None))
        lines += ["", "## " + title, ""]
        if page:
            lines.append("Module page: [%s](%s) — give everything: `%s`" % (title, page, GIVE.get(namespace, "n/a")))
            lines.append("")
        lines += ["| Item | Recipe | Station | Namespace id |", "| ---- | ------ | ------- | ------------ |"]
        for _, name, display, station, image in sorted(by_module[namespace], key=lambda r: r[2].lower()):
            picture = "![%s recipe](%s){ width=\"220\" }" % (display, image) if image else "_no picture yet_"
            lines.append("| **%s** | %s | %s | `%s:%s` |" % (display, picture, station, namespace, name))

    if enchant:
        lines += [
            "",
            "## Not crafted — won on an enchanting table",
            "",
            "These have no recipe. Drop the item on top of a plain enchanting table and",
            "each one rolls: a hit becomes the upgrade, a miss is destroyed. One item a",
            "second, so a stack of 64 is 64 separate rolls. Full explanation on",
            "[Enchant Crafting](enchant-crafting.md).",
            "",
            "| Sacrifice | Becomes | Chance | Module |",
            "| --------- | ------- | ------ | ------ |",
        ]
        for namespace, sacrifice, result, chance in sorted(enchant, key=lambda r: r[2].lower()):
            title, page = MODULES.get(namespace, (namespace, None))
            module = "[%s](%s)" % (title, page) if page else title
            lines.append("| %s | **%s** | %s%% | %s |" % (sacrifice, result, chance, module))

    lines.append("")
    pathlib_target = os.path.join(docs_root, target)
    open(pathlib_target, "w").write("\n".join(lines))
    return pathlib_target
