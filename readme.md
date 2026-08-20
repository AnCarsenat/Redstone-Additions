<p align="center">
  <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/icon.png" alt="Redstone Additions" width="128">
</p>

<h1 align="center">Redstone Additions</h1>

<p align="center">
  <strong>57 custom blocks. 7 tools. 66 recipes. 2 jetpacks and 3 upgrade kits. Vanilla datapack for Minecraft 1.21.9–26.2.</strong>
</p>

<p align="center">
  <a href="https://modrinth.com/datapack/redstone-additions"><img src="https://img.shields.io/badge/Download-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth"></a>
  <a href="https://github.com/AnCarsenat/Redstone-Additions"><img src="https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.9--26.2-2EA44F" alt="Minecraft 1.21.9-26.2">
  <img src="https://img.shields.io/badge/Version-v5.1.16-blue" alt="Version v5.1.16">
  <img src="https://img.shields.io/badge/Blocks-57-red" alt="57 blocks">
  <img src="https://img.shields.io/badge/Tools-7-lightgrey" alt="7 tools">
  <img src="https://img.shields.io/badge/Recipes-66-8A2BE2" alt="66 recipes">
</p>

---

Redstone Additions expands vanilla redstone with logic gates, automation machines, storage, sensors, wireless signaling, chunk loading, multiblocks, and liquid/gas/electric transport networks.

It also goes beyond redstone: **jetpacks** that fit onto any chestplate, **enchant crafting** that gambles items on an enchanting table, **generators** that regrow their own material, and **ender links** that move items, fluids, power and players between distant places.

All systems run as a pure datapack using marker entities and function-driven runtime logic. No mods required.

## Version Compatibility

**Supported: Minecraft 1.21.9 – 26.2** (data pack formats 88 – 107).

The pack declares and loads across that whole span. Exactly one thing inside it
genuinely changes shape: format 102 (`26.2-snapshot-3`) rewrote entity
predicates into component-map form, so `ra:is_sneaking` — which gates the
wrench's shift action, the goggles' tinker action and the Redstone Remote's
channel prompt — needs both spellings. The pack ships both: the base file uses
the pre-102 form and `overlay_102/` carries the component-map form, applied
automatically from `26.2-snapshot-3` onward. Nothing else in the pack touches a
feature that broke between 88 and 107 — no `filtered` loot function, no
`contents` dynamic entry, no renamed game rules, no special crafting recipes.

Earlier versions are **not supported**, but most of the pack does not actually
need 1.21.9 — the hard floor is the `pack.mcmeta` schema. If you edit
`pack.mcmeta` to a lower `pack_format` and drop `min_format`/`max_format`, this
is what breaks and when:

| Below | What stops working |
|---|---|
| 1.21.9 | Nothing in the content itself — only the `min_format`/`max_format` fields, which that version introduced |
| 1.21.5 | Item Pipe **filters**, which identify their item frame by `block_pos` |
| 1.21.2 | Almost every item's appearance and identity (`item_model`, `consumable`, component removal) |
| 1.20.5 | Setting the Redstone Remote's channel (`copy_custom_data`) |
| 1.20.2 | Everything — the pack is built on macro functions and `return run` |

So in practice the content runs on 1.21.5+ with only the pipe filters missing,
and on 1.21.2+ in a degraded state. None of that is declared or supported.


I recommend using [Bundles Beyond](https://modrinth.com/mod/bundles-beyond) for better bundle previews.

---

## Module Previews

One banner per module. Modules without a screenshot yet show a placeholder —
**contributions welcome**, drop a PNG in `docs/images/screenshots/` and swap the
row.

**Every recipe in the pack is on one page in the
[Recipe Atlas](https://ancarsenat.github.io/Redstone-Additions/recipe-atlas/)** —
listed A to Z and grouped by module.

| Module | Banner |
|---|---|
| Logic Gates | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Interactive Machines | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Storage | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Sensors | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Wireless Redstone | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Transport Networks | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/screenshots/Electricity.png" alt="An EU grid running" width="320"> |
| &nbsp;&nbsp;↳ fluids | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/screenshots/Pipes.png" alt="A fluid network" width="320"> |
| &nbsp;&nbsp;↳ Electric Furnace | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/screenshots/ElectricFurnace.png" alt="Electric Furnace smelting on EU" width="320"> |
| &nbsp;&nbsp;↳ Industrial Light | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/screenshots/IndustrialLightOn.png" alt="Industrial Light casting its beam" width="320"> |
| Chunk Loader | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Multiblocks | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/ra_multiblock/blast_forge.png" alt="Blast Forge" width="320"> |
| Enchant Crafting | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Jetpacks | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/screenshots/JetpackHover.png" alt="Hover flight" width="320"> |
| Infinite Generators | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Ender Links | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |
| Tools | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/banners/media-missing.png" alt="No screenshot yet" width="320"> |

---

## Blocks

### Logic Gates (6)
- UNI Gate
- Clock
- Delayer
- Extender
- Randomizer
- Shortener

### Interactive Machines (11)
- Block Breaker
- Block Placer
- Item Pipe
- Item Mover
- Spitter
- Breeder
- Magic Crate — pulls dropped items in from 5-20 blocks
- Infinite Water Cauldron
- Infinite Lava Cauldron
- Infinite Snow Cauldron
- Message Block

### Storage (2)
- Boxer
- Unboxer

### Sensors (3)
- Entity Detector
- Tag Adder
- Tag Remover

### Wireless (2)
- Wireless Emitter
- Wireless Receiver

### Transport Networks (20)

Fluids, in millilitres:

- Copper Pipe
- Liquid Tank
- Liquid Pump
- Liquid Valve — moves contents between the two networks it separates
- Liquid Drain — world source in, or network contents back out
- Gas Tank
- Gas Pump
- Gas Valve
- Boiler — water plus heat to steam

Electricity, in EU:

- Wire
- EU Generator — a barrel wearing a furnace; drop fuel in, no smelting slot
- EU Consumer
- EU Switch — redstone conducts, no redstone cuts
- Battery — 10000 EU of grid capacity
- EU Breaker — evens EU between the grids either side
- Solar Panel
- Industrial Light — redstone plus EU, a 10 block beam of real light
- Electric Furnace — smelts on EU with no fuel, four power modes

Creative, for building and testing:

- Creative EU Source — refills its grid to capacity every tick
- Creative Fluid Source — fills with a medium cycled by the wrench

There are no tiers. A netherite pipe was a more expensive way to buy the same
block, so the tiered items now place the single version.

### Chunk Loader (1)
- Chunk Loader

### Infinite Generators (3)
- Mineral Generator
- Nether Generator
- Poppy Generator

Built from a crafted Generator Casing plus a Core won by sacrificing stone,
netherrack or poppies on an enchanting table.

### Ender Links (4)
- Ender Item Vault — barrels that share their contents across a channel
- Ender Fluid Vault — liquid and gas across a channel, as a fluid network node
- Ender Power Vault — EU across a channel, as an electric node
- Teleport Anchor — redstone strength 1-15 picks which anchor you arrive at

### Multiblock Bases (5)
- Copper Multiblock Base
- Iron Multiblock Base
- Gold Multiblock Base
- Diamond Multiblock Base
- Netherite Multiblock Base

### Items (10)
- Iron Jetpack Kit — right-click while wearing any chestplate to fit it
- Infinite Iron Jetpack Kit — won on an enchanting table; burns no fuel
- Thruster Kit — hold sprint while airborne to accelerate
- Lift Kit — climbs and sinks about twice as fast
- Scorch Kit — sets fire to what flies under you
- Generator Casing
- Mineral Core
- Nether Core
- Poppy Core
- Item Crate — what the Boxer packs a chest into, and the Unboxer unpacks

Fitted kits live on the chestplate and are listed in its lore.
`/trigger ra.jp.kits` switches one off or takes it back as an item.

### Tools (7)
- Wrench — the only tool that **changes** a block. Shift+RMB cycles a setting, or
  opens a menu when the block has more than one
- Goggles — wear or hold to read every block in range. Read-only
- Data Handler — edits one block's properties in detail
- Creative Data Handler — edits anything, including what the block owns
- Clipboard — copies one block's settings onto others of the same kind
- Multimeter — reads a network's numbers into chat
- Redstone Remote — wireless channel control

See the [Tools page](https://ancarsenat.github.io/Redstone-Additions/tools/) for
recipes and usage.

---

## Development Setup

This project uses **[Beet](https://github.com/mcbeet/beet)** to build the datapack from source.

### Prerequisites
- Python 3.10 or higher
- Git

### Quick Start

#### 1. Clone the Repository
```bash
git clone https://github.com/AnCarsenat/Redstone-Additions.git
cd Redstone-Additions
git checkout beet_rewrite
```

#### 2. Set Up the Virtual Environment
Navigate to the `redstone_additions/` directory and create a Python virtual environment:

```bash
cd redstone_additions
python3 -m venv .venv
```

#### 3. Activate the Virtual Environment
**On Linux/macOS:**
```bash
source .venv/bin/activate
```

**On Windows (CMD):**
```bash
.venv\Scripts\activate.bat
```

**On Windows (PowerShell):**
```bash
.venv\Scripts\Activate.ps1
```

#### 4. Install Dependencies
```bash
pip install -r requirements.txt
```

#### 5. Link to Your Minecraft World
Use beet's `link` command to connect to your Minecraft world. Specify the path to your Minecraft installation and the world name:

```bash
beet link --minecraft /path/to/minecraft <world_name>
```

**Example with PrismLauncher:**
```bash
beet link --minecraft "~/.local/share/PrismLauncher/instances/26.2 Fabric/minecraft" DevWorld
```

This creates a symbolic link between the built datapack and your world's datapacks folder.

#### 6. Build and Test
Build the datapack from source:

```bash
beet build
```

The compiled datapack will be generated in the `build/` directory and automatically linked to your world.

To enable auto-rebuild when you make changes, use watch mode:

```bash
beet watch
```

Then use `/reload` in-game to apply changes.

### Development Workflow

1. Navigate to the `redstone_additions/` directory
2. Ensure the virtual environment is activated: `source .venv/bin/activate`
3. Edit files in the `src/` directory
4. Use `beet watch` for automatic rebuilding on file changes
5. Use `/reload` in-game to apply changes immediately

---

## Installation

### Option A (Modrinth)
1. Download the latest `.zip` from [Modrinth](https://modrinth.com/datapack/redstone-additions).
2. Place it in `.minecraft/saves/<your_world>/datapacks/`.
3. Run `/reload`.

### Option B (GitHub)
```bash
git clone https://github.com/AnCarsenat/Redstone-Additions.git
```

Copy `redstone_additions` into your world `datapacks/` directory, then run `/reload`.

Starter command:

```mcfunction
/function ra:give_all_items
```

---

## Quick Links

| Link | URL |
|---|---|
| Docs Home | https://ancarsenat.github.io/Redstone-Additions/ |
| Logic Gates | https://ancarsenat.github.io/Redstone-Additions/logic-gates/ |
| Interactive Machines | https://ancarsenat.github.io/Redstone-Additions/interactive-machines/ |
| Storage | https://ancarsenat.github.io/Redstone-Additions/storage/ |
| Sensors | https://ancarsenat.github.io/Redstone-Additions/sensors/ |
| Wireless Redstone | https://ancarsenat.github.io/Redstone-Additions/wireless-redstone/ |
| Transport Networks | https://ancarsenat.github.io/Redstone-Additions/transport-networks/ |
| Tools | https://ancarsenat.github.io/Redstone-Additions/tools/ |
| Chunk Loader | https://ancarsenat.github.io/Redstone-Additions/chunk-loader/ |
| Multiblocks | https://ancarsenat.github.io/Redstone-Additions/multiblocks/ |
| Enchant Crafting | https://ancarsenat.github.io/Redstone-Additions/enchant-crafting/ |
| Jetpacks | https://ancarsenat.github.io/Redstone-Additions/jetpacks/ |
| Infinite Generators | https://ancarsenat.github.io/Redstone-Additions/infinite-generators/ |
| Ender Links | https://ancarsenat.github.io/Redstone-Additions/ender-links/ |
| Block Reference | https://ancarsenat.github.io/Redstone-Additions/block-reference/ |
| Recipe Reference | https://ancarsenat.github.io/Redstone-Additions/recipe-reference/ |
| Changelog | https://ancarsenat.github.io/Redstone-Additions/changelog/ |

---

## Useful Commands

| Command | Purpose |
|---|---|
| `/function ra:give_all_items` | One prefilled bundle per namespace |
| `/function ra_gates:items/give_all` | Logic gate items |
| `/function ra_interactive:items/give_all` | Interactive machine items |
| `/function ra_storage:items/give_all` | Storage items |
| `/function ra_sensors:items/give_all` | Sensor items |
| `/function ra_wireless:items/give_all` | Wireless items |
| `/function ra_wires:items/give_all` | Transport and EU items |
| `/function ra_chunk_loader:items/give_all` | Chunk loader items |
| `/function ra_multiblock:blocks/give_all` | Multiblock base items |
| `/function ra_infinite:items/give_all` | Generator casing and generators |
| `/function ra_jetpacks:items/give_all` | Jetpack kits |
| `/function ra_ender:items/give_all` | Ender vaults and Teleport Anchors |
| `/function ra:tools/wrench/give` | Wrench |
| `/function ra:tools/data_handler/give` | Data Handler |
| `/function ra:tools/creative_data_handler/give` | Creative Data Handler |
| `/function ra:tools/goggles/give` | Goggles |
| `/function ra_wireless:tools/remote/give` | Redstone Remote |
| `/function ra:uninstall` | Clean uninstall |

---

## Documentation

- Wiki : [https://ancarsenat.github.io/Redstone-Additions/](https://ancarsenat.github.io/Redstone-Additions/)
- Build config: [mkdocs.yml](https://github.com/AnCarsenat/Redstone-Additions/raw/main/mkdocs.yml)

---

## License

Copyright (c) 2026 AnCarsenat. All rights reserved. See
[LICENSE](https://github.com/AnCarsenat/Redstone-Additions/raw/main/LICENSE) for the
full terms — the short version:

- **Play with it freely** on any world, server or realm, including commercial servers.
- **Modify your own copy** as much as you like.
- **Do not redistribute it** — no reuploads, mirrors, repackages or modpack bundles of
  the pack or any part of it, modified or not, without written permission. Link to the
  official pages instead.
- **Addons are welcome and need no permission.** A separate pack of your own that
  depends on RA, in your own namespaces, containing no RA namespace, is yours: publish
  it anywhere, on any terms, and use the snippets from the docs and `ra_example` to
  build it.

Redstone Additions is fan content and is not affiliated with Mojang or Microsoft. The
recipe pictures under `docs/images/recipes/` are drawn from Minecraft's own textures,
which remain Mojang's property and are used under their asset guidelines.

---

<p align="center">
  <strong>Created by <a href="https://github.com/AnCarsenat">AnCarsenat</a></strong><br>
  <a href="https://github.com/AnCarsenat/Redstone-Additions/issues">Report a Bug</a> · <a href="https://ancarsenat.github.io/Redstone-Additions/">Documentation</a> · <a href="https://modrinth.com/datapack/redstone-additions">Download</a>
</p>

