<p align="center">
  <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/icon.png" alt="Redstone Additions" width="128">
</p>

<h1 align="center">Redstone Additions</h1>

<p align="center">
  <strong>45 custom blocks. 5 tools. 50 recipes. Vanilla datapack for Minecraft 1.21.9–26.2.</strong>
</p>

<p align="center">
  <a href="https://modrinth.com/datapack/redstone-additions"><img src="https://img.shields.io/badge/Download-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth"></a>
  <a href="https://github.com/AnCarsenat/Redstone-Additions"><img src="https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.9--26.2-2EA44F" alt="Minecraft 1.21.9-26.2">
  <img src="https://img.shields.io/badge/Version-v5.1.4-blue" alt="Version v5.1.4">
  <img src="https://img.shields.io/badge/Blocks-45-red" alt="45 blocks">
  <img src="https://img.shields.io/badge/Tools-5-lightgrey" alt="5 tools">
  <img src="https://img.shields.io/badge/Recipes-50-8A2BE2" alt="50 recipes">
</p>

---

Redstone Additions expands vanilla redstone with logic gates, automation machines, storage, sensors, wireless signaling, chunk loading, multiblocks, and liquid/gas/electric transport networks.

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

## Default Module Previews
*Does not include all recipes* go to [Wiki](https://ancarsenat.github.io/Redstone-Additions) for all recipes.
| Module | Preview |
|---|---|
| Logic Gates | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_gates/clock.png" alt="Clock recipe" width="220"> |
| Interactive Machines | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_interactive/block_placer.png" alt="Block placer recipe" width="220"> |
| Sensors | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_sensors/entity_detector.png" alt="Entity detector recipe" width="220"> |
| Wireless Redstone | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_wireless/emitter.png" alt="Emitter recipe" width="220"> |
| Chunk Loader | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_chunk_loader/chunk_loader.png" alt="Chunk loader recipe" width="220"> |
| Multiblocks | <img src="https://github.com/AnCarsenat/Redstone-Additions/raw/main/docs/images/recipes/ra_multiblock/copper_base.png" alt="Copper multiblock base recipe" width="220"> |

---

## Blocks

### Logic Gates (6)
- UNI Gate
- Clock
- Delayer
- Extender
- Randomizer
- Shortener

### Interactive Machines (10)
- Block Breaker
- Block Placer
- Item Pipe
- Item Mover
- Spitter
- Breeder
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

### Transport Networks (16)
- Copper Liquid Pipe
- Netherite Liquid Pipe
- Liquid Tank
- Liquid Pump
- Liquid Valve
- Liquid Drain
- Copper Gas Pipe
- Netherite Gas Pipe
- Gas Tank
- Gas Pump
- Gas Valve
- Copper Electric Wire
- Netherite Electric Wire
- EU Generator
- EU Consumer
- EU Switch

### Chunk Loader (1)
- Chunk Loader

### Multiblock Bases (5)
- Copper Multiblock Base
- Iron Multiblock Base
- Gold Multiblock Base
- Diamond Multiblock Base
- Netherite Multiblock Base

### Tools (5)
- Wrench
- Creative Data Handler
- Data Handler
- Goggles
- Redstone Remote

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
| Chunk Loader | https://ancarsenat.github.io/Redstone-Additions/chunk-loader/ |
| Multiblocks | https://ancarsenat.github.io/Redstone-Additions/multiblocks/ |
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

Copyright (c) 2026 AnCarsenat. All rights reserved.

You may use this datapack on any world or server and modify it for private use. You may not redistribute, mirror, reupload, or repackage it in whole or in part without explicit written permission.

See [LICENSE](https://github.com/AnCarsenat/Redstone-Additions/raw/main/LICENSE) for full terms.

---

<p align="center">
  <strong>Created by <a href="https://github.com/AnCarsenat">AnCarsenat</a></strong><br>
  <a href="https://github.com/AnCarsenat/Redstone-Additions/issues">Report a Bug</a> · <a href="https://ancarsenat.github.io/Redstone-Additions/">Documentation</a> · <a href="https://modrinth.com/datapack/redstone-additions">Download</a>
</p>

