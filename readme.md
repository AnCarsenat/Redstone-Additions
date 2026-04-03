<p align="center">
  <img src="icon.png" alt="Redstone Additions V3" width="128">
</p>

<h1 align="center">Redstone Additions V3</h1>

<p align="center">
  <strong>28 custom blocks. 4 tools. Vanilla datapack for Minecraft 1.21+.</strong>
</p>

<p align="center">
  <a href="https://modrinth.com/datapack/redstone-additions-v3"><img src="https://img.shields.io/badge/Download-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth"></a>
  <a href="https://github.com/AnCarsenat/Redstone-Additions-V3"><img src="https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-2EA44F?logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiI+PHJlY3QgZmlsbD0iIzVBOTEzMiIgd2lkdGg9IjE2IiBoZWlnaHQ9IjE2Ii8+PC9zdmc+" alt="Minecraft 1.21+">
  <img src="https://img.shields.io/badge/Version-4.0.0-blue" alt="Version 4.0.0">
  <img src="https://img.shields.io/badge/Blocks-28-red" alt="28 blocks">
  <img src="https://img.shields.io/badge/Tools-4-lightgrey" alt="4 tools">
  <img src="https://img.shields.io/badge/License-Custom-orange" alt="Custom License">
</p>

---

Redstone Additions V3 is a modular datapack that expands vanilla redstone with custom logic blocks, automation machines, sensors, wireless signaling, chunk loading, and multiblock systems.

Everything runs in vanilla using datapack functions and custom block markers. No mods required.

---

## Block and Tool Inventory

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
- Pusher
- Breeder
- Infinite Water Cauldron
- Infinite Lava Cauldron
- Infinite Snow Cauldron
- Message Block

### Sensors (3)
- Entity Detector
- Tag Adder
- Tag Remover

### Wireless (2 blocks + 1 tool)
- Wireless Emitter
- Wireless Receiver
- Redstone Remote (tool)

### Chunk Loader (1)
- Chunk Loader

### Multiblock Bases (5)
- Copper Multiblock Base
- Iron Multiblock Base
- Gold Multiblock Base
- Diamond Multiblock Base
- Netherite Multiblock Base

### Tools (4 total)
- Wrench
- Creative Data Handler
- Goggles
- Redstone Remote

---

## Installation

### Option A (Modrinth)
1. Download the latest `.zip` from [Modrinth](https://modrinth.com/datapack/redstone-additions-v3).
2. Place it in `.minecraft/saves/<your_world>/datapacks/`.
3. Run `/reload`.

### Option B (GitHub)
```bash
git clone https://github.com/AnCarsenat/Redstone-Additions-V3.git
```
Copy `redstone_additions_v3.0.0` into your world `datapacks/` directory, then run `/reload`.

Get all items:
```
/function ra:give_all_items
```

---

## Useful Commands

| Command | What it does |
|---------|--------------|
| `/function ra:give_all_items` | Every block and tool in your inventory |
| `/function ra_gates:items/give_all` | All logic gate items |
| `/function ra_interactive:items/give_all` | All interactive machines |
| `/function ra_sensors:items/give_all` | All sensor items |
| `/function ra_wireless:items/give_all` | All wireless items |
| `/function ra_chunk_loader:items/give_all` | Chunk loader items |
| `/function ra_multiblock:blocks/give_all` | Multiblock base items |
| `/function ra:tools/wrench/give` | The Wrench |
| `/function ra:tools/creative_data_handler/give` | The Creative Data Handler |
| `/function ra:tools/goggles/give` | Goggles |
| `/function ra:uninstall` | Clean uninstall — removes all markers |

---

## How It Works

Custom blocks are represented as bat spawn eggs with custom data. On placement, the spawned bat is immediately converted into:
- a real vanilla block (for example dispenser, dropper, lodestone), and
- a hidden marker entity that stores runtime properties and state.

Module tick functions process those markers each game tick.

The codebase is split by namespaces (`ra`, `ra_lib`, `ra_lib_multiblock`, `ra_gates`, `ra_interactive`, `ra_sensors`, `ra_wireless`, `ra_chunk_loader`, `ra_multiblock`).

If you are extending the pack, start with the Developer Guide and Extension Examples pages.

---

## Documentation

Wiki pages:

| Page | Content |
|------|---------|
| [Logic Gates](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Logic-Gates) | UNI Gate, Clock, Delayer, Extender, Shortener, Randomizer |
| [Interactive Machines](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Interactive-Machines) | Block Breaker, Block Placer, Item Pipe, Item Mover, Spitter, Pusher, Breeder, cauldrons, Message Block |
| [Sensors](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Sensors) | Entity Detector, Tag Adder, Tag Remover |
| [Wireless Redstone](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Wireless-Redstone) | Emitter, Receiver, Redstone Remote |
| [Chunk Loader](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Chunk-Loader) | Chunk loading block behavior |
| [Multiblocks](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Multiblocks) | Base tiers and assembled structures |
| [Block Reference](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Block-Reference) | Full block matrix |
| [Recipe Reference](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Recipe-Reference) | Recipe file mapping |
| [Developer Guide](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Developer-Guide) | Architecture and conventions |
| [Extension Examples](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Extension-Examples) | Copy-paste custom block examples |
| [Changelog](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Changelog) | Full version history |

---

## Latest Changelog

### v4.0.0 — February 2026
- Added Goggles tool
- Added chunk loader documentation and module integration updates
- Added Blast Forge heat system and balancing
- Added uninstall confirmation flow
- Refined naming consistency and cleanup for legacy/stale content

---

## Contributing

Pull requests are welcome. If you add a new block, follow the conventions in:
- [Developer Guide](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Developer-Guide)
- [Extension Examples](https://github.com/AnCarsenat/Redstone-Additions-V3/wiki/Extension-Examples)

For bug reports, open an [issue on GitHub](https://github.com/AnCarsenat/Redstone-Additions-V3/issues).

---

## License

**Copyright (c) 2026 AnCarsenat. All rights reserved.**

You are free to **use** this datapack on any world or server and to **modify** it for private use. You may **not** redistribute, reupload, mirror, or repackage it — in whole or in part — on any platform without explicit written permission from the author. Linking to the official [Modrinth](https://modrinth.com/datapack/redstone-additions-v3) or [GitHub](https://github.com/AnCarsenat/Redstone-Additions-V3) pages is always permitted.

If you feature this datapack in videos, streams, or articles, please credit **AnCarsenat** and link to the official repository.

See the full [LICENSE](LICENSE) file for complete terms.

---

<p align="center">
  <strong>Created by <a href="https://github.com/AnCarsenat">AnCarsenat</a></strong><br>
  <a href="https://github.com/AnCarsenat/Redstone-Additions-V3/issues">Report a Bug</a> · <a href="https://github.com/AnCarsenat/Redstone-Additions-V3/wiki">Wiki</a> · <a href="https://modrinth.com/datapack/redstone-additions-v3">Download</a>
</p>
