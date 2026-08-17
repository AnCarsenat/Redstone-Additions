# Extension Examples


!!! info "These examples are yours to copy"
    Everything on this page, and the whole `ra_example` namespace, is here to be
    copied into an addon of your own. The
    [licence](https://github.com/AnCarsenat/Redstone-Additions/blob/main/LICENSE)
    permits addons and the snippets they need without asking; what it does not permit
    is redistributing Redstone Additions itself.

This page contains copy-paste-ready examples for building on Redstone Additions.

All examples follow conventions from `GUIDELINES.md` and current v4 runtime behavior.

Read [How It Works](how-it-works.md) first if you need lifecycle context for these snippets.

## Example 1: Simple Custom Block (Pulse Beacon)

Goal: place a block that emits a short pulse when powered.

### File Tree

```text
data/ra_myaddon/
  function/
    load.mcfunction
    tick.mcfunction
    items/
      give_all.mcfunction
    blocks/
      pulse_beacon/
        give.mcfunction
        handle_placement.mcfunction
        register_block.mcfunction
        tick.mcfunction
        on_powered.mcfunction
        reset.mcfunction
        on_break.mcfunction
  recipe/
    pulse_beacon.json
```

### load.mcfunction

```mcfunction
# /ra_myaddon:load
function ra_myaddon:blocks/pulse_beacon/register_block
```

### tick.mcfunction

```mcfunction
# /ra_myaddon:tick
function ra_myaddon:blocks/pulse_beacon/tick
```

### items/give_all.mcfunction

```mcfunction
# /ra_myaddon:items/give_all
function ra_myaddon:blocks/pulse_beacon/give
```

### blocks/pulse_beacon/give.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/give
give @s bat_spawn_egg[item_model="minecraft:lightning_rod",item_name="Pulse Beacon",custom_data={ra:{pulse_beacon:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.pulse_beacon"],Silent:1b,NoAI:1b,Invulnerable:1b}]
```

### blocks/pulse_beacon/handle_placement.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/handle_placement
execute unless entity @s[tag=ra.place.pulse_beacon] run return 0
function ra_lib:placement/place {block_id:"minecraft:lightning_rod",block_tag:"pulse_beacon",dir_type:0}
return 1
```

### blocks/pulse_beacon/register_block.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/register_block
tellraw @a[tag=ra.debug] [{"text":"[RA] ","color":"gold"},{"text":"Pulse Beacon registered","color":"gray"}]
```

### blocks/pulse_beacon/tick.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/tick

# Break detection
execute as @e[tag=ra.custom_block.pulse_beacon] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.pulse_beacon] at @s run function ra_myaddon:blocks/pulse_beacon/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.pulse_beacon] remove ra.broken

# Detect redstone and trigger on rising edge
execute as @e[tag=ra.custom_block.pulse_beacon] at @s run function ra_lib:redstone/detect
execute as @e[tag=ra.custom_block.pulse_beacon,tag=ra.powered,tag=!ra.was_powered] at @s run function ra_myaddon:blocks/pulse_beacon/on_powered
tag @e[tag=ra.custom_block.pulse_beacon,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.pulse_beacon,tag=!ra.powered] remove ra.was_powered
```

### blocks/pulse_beacon/on_powered.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/on_powered
# Emit a short pulse in the surrounding shell
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
schedule function ra_myaddon:blocks/pulse_beacon/reset 4t
```

### blocks/pulse_beacon/reset.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/reset
execute as @e[tag=ra.custom_block.pulse_beacon] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
```

### blocks/pulse_beacon/on_break.mcfunction

```mcfunction
# /ra_myaddon:blocks/pulse_beacon/on_break
kill @e[type=item,nbt={Item:{id:"minecraft:lightning_rod"}},distance=..2,limit=1,sort=nearest]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:lightning_rod","minecraft:item_name":"Pulse Beacon","minecraft:custom_data":{ra:{pulse_beacon:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.pulse_beacon"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
```

### recipe/pulse_beacon.json

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "redstone",
  "pattern": [
    " C ",
    "RLR",
    " C "
  ],
  "key": {
    "C": "minecraft:copper_ingot",
    "R": "minecraft:redstone",
    "L": "minecraft:lightning_rod"
  },
  "result": {
    "id": "minecraft:bat_spawn_egg",
    "count": 1,
    "components": {
      "minecraft:item_model": "minecraft:lightning_rod",
      "minecraft:item_name": "Pulse Beacon",
      "minecraft:custom_data": { "ra": { "pulse_beacon": true } },
      "minecraft:entity_data": {
        "id": "minecraft:bat",
        "Tags": ["ra.spawned", "ra.place.pulse_beacon"],
        "Silent": true,
        "NoAI": true,
        "Invulnerable": true
      }
    }
  },
  "show_notification": true
}
```

### Required Registration Hooks

Add your placement handler into:

- `data/ra/tags/function/placement_handlers.json`

Also call your namespace load/tick from your own entrypoints, and make sure your pack entrypoint is wired through tags if this is a standalone addon datapack.

## Example 2: Addon Namespace Scaffold

Minimal scaffold for a full addon namespace with two blocks:

```text
data/ra_myaddon/
  function/
    load.mcfunction
    tick.mcfunction
    items/give_all.mcfunction
    blocks/
      alpha/{give,handle_placement,register_block,tick,on_break}.mcfunction
      beta/{give,handle_placement,register_block,tick,on_break}.mcfunction
  recipe/
    alpha.json
    beta.json
  advancement/
    tools/
      get_alpha.json
      get_beta.json
```

Recommended load pattern:

```mcfunction
# /ra_myaddon:load
function ra_myaddon:blocks/alpha/register_block
function ra_myaddon:blocks/beta/register_block
```

Recommended tick pattern:

```mcfunction
# /ra_myaddon:tick
function ra_myaddon:blocks/alpha/tick
function ra_myaddon:blocks/beta/tick
```

## Example 3: Multiblock Extension Pattern

Goal: add a new multiblock type using existing `ra_lib_multiblock` lifecycle.

### Files to Add

```text
data/ra_multiblock/function/my_structure/
  validate.mcfunction
  check_structure.mcfunction
  setup_type.mcfunction
  tick.mcfunction
  on_break.mcfunction
```

### validate.mcfunction

```mcfunction
# /ra_multiblock:my_structure/validate
# Set ra.multiblock result objective to 1 when valid, 0 when invalid.
scoreboard players set #mb_result ra.multiblock 0

# Example minimal check: require blast_furnace at marker position
execute if block ~ ~ ~ minecraft:blast_furnace run scoreboard players set #mb_result ra.multiblock 1
```

### setup_type.mcfunction

```mcfunction
# /ra_multiblock:my_structure/setup_type
# Context: as marker
# Initialize typed data used by tick/process.
data modify entity @s data.type set value "my_structure"
data modify entity @s data.properties set value {enabled:1b}
```

### tick.mcfunction

```mcfunction
# /ra_multiblock:my_structure/tick
# Context: as marker
execute if data entity @s data.properties{enabled:1b} run particle minecraft:happy_villager ~ ~1 ~ 0.25 0.25 0.25 0.01 2
```

### Hook Registration

Register your type in relevant `ra_lib_multiblock` function tags:

- validate hook
- setup hook
- tick hook
- on_break hook

Also ensure wrench assembly flow can set your intended type in `storage ra:multiblock` before assembly.

## Sanity Checklist For New Blocks

1. Give item custom_data matches placement handler tag.
2. Placement handler returns success only for matching bats.
3. Tick includes break cleanup.
4. Recipe output entity tags exactly match give item tags.
5. CDH and Goggles are extended if your block has properties/status.
6. `ra:placement_handlers` includes your new handler.

---
