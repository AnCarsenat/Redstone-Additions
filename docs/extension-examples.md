# Extension Examples


!!! info "These examples are yours to copy"
    Everything on this page, and the whole `ra_example` namespace, is here to be
    copied into an addon of your own. The
    [licence](https://github.com/AnCarsenat/Redstone-Additions/blob/main/LICENSE)
    permits addons and the snippets they need without asking; what it does not permit
    is redistributing Redstone Additions itself.

There are two ways to build on Redstone Additions:

| You are… | Write | Start from |
| --- | --- | --- |
| adding to the pack itself (a contribution) | Kotlin in `redstone_additions/` | [Contributing Guidelines](contributing-guidelines.md#block-template), [Custom Blocks](custom-blocks.md) |
| shipping a separate **addon** data pack in your own namespace | plain `.mcfunction` / JSON (or your own Kore project) | this page |

An addon talks to the pack only through its runtime contract — the `#ra:placement_handlers`
tag, `ra_lib:*` functions, entity tags, scoreboards and storages — and that contract is exactly
what it was before the pack moved to Kotlin, so the snippets below are plain data pack files.
They follow the [naming conventions](contributing-guidelines.md#naming-conventions). The
`ra_example` namespace that ships with the pack (`ra/example/ExamplePart.kt`) is a complete
worked example.

Read [How It Works](how-it-works.md) first if you need lifecycle context for these snippets.

!!! note "26.3 only"
    The pack targets Minecraft 26.3 (data pack format 121). Write an addon's JSON in the 26.3
    shapes: predicates and item modifiers name their type with `type`, and the
    `recipe_unlocked` trigger takes `recipes`.

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

Add your placement handler to the `#ra:placement_handlers` function tag **from your own pack**:
a file `data/ra/tags/function/placement_handlers.json` in your addon with `"replace": false`
and your handler as its only value. Tags from several packs merge, so this adds to the pack's
list instead of replacing it:

```json
{ "replace": false, "values": ["ra_myaddon:blocks/pulse_beacon/handle_placement"] }
```

`ra:load` and `ra:tick` only call the pack's own modules. Run your namespace's load from
`#minecraft:load` and your tick from `#minecraft:tick` (or your own `schedule` loop).

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
```

### tick.mcfunction

```mcfunction
# /ra_multiblock:my_structure/tick
# Context: as marker
# A redstone block under the marker pauses it (the shipped machines use their redstone control).
execute if block ~ ~-1 ~ minecraft:redstone_block run return 0
particle minecraft:happy_villager ~ ~1 ~ 0.25 0.25 0.25 0.01 2
```

### Hook Registration

Register your type in relevant `ra_lib_multiblock` function tags:

- validate hook
- setup hook
- tick hook
- on_break hook

Also ensure wrench assembly flow can set your intended type in `storage ra:multiblock` before assembly.

## Declaring your block's properties

The Data Handler, the wrench and the clipboard read each block type's property schema from
`storage ra:dh types."<data.type>"` and the menu order from `order."<data.type>"`. Redstone
Additions writes its own types one by one on load, so an add-on writes its types the same way in
its own load function, in either order:

```mcfunction
# /ra_myaddon:load
data modify storage ra:dh types."ra_myaddon.pulse_beacon" set value {interval:{type:"number",code:1,min:1,max:200,readonly:0b,admin_only:0b,visible:1b,i:0},owner_only:{type:"bool",code:2,readonly:0b,admin_only:1b,visible:1b,i:1}}
data modify storage ra:dh order."ra_myaddon.pulse_beacon" set value ["interval","owner_only"]
```

`code` is 0 text, 1 number, 2 bool, 3 list, 4 item id; the flags are bytes. A list also takes
`of:"text",of_code:0`, `max_length` and `pad:1b`. A property you do not declare still works, but
the Data Handler only guesses its type, and flags its row with a warning. See
[Property schemas](tools-and-registries.md#property-schemas).

## Sanity Checklist For New Blocks

1. Give item custom_data matches placement handler tag.
2. Placement handler returns success only for matching bats.
3. Tick includes break cleanup.
4. Recipe output entity tags exactly match give item tags.
5. Your block's properties are declared (see below) and Goggles are extended if it has status.
6. `#ra:placement_handlers` includes your new handler (a `"replace": false` tag file in your addon).

---
