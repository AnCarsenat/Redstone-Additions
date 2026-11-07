# Sensors

Blocks that react to entities: one detects them, two add or remove scoreboard tags on them. Handy
for adventure maps, mob farms and trigger zones.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |
| Entity Detector | waxed chiseled copper | ![Entity Detector recipe](images/recipes/ra_sensors/entity_detector.png){ width="220" } |
| Tag Adder | green glazed terracotta | ![Tag Adder recipe](images/recipes/ra_sensors/tag_adder.png){ width="220" } |
| Tag Remover | red glazed terracotta | ![Tag Remover recipe](images/recipes/ra_sensors/tag_remover.png){ width="220" } |

Operators: `/function ra_sensors:items/give_all`.

## Using them

All three are configured with a **target selector** (`entity_selector`), the same syntax as
commands: `@e[type=zombie,distance=..8]`, `@a[tag=red_team]`, … Edit it with the
[Data Handler](tools.md).

| Block | When | Does | Default settings |
| --- | --- | --- | --- |
| Entity Detector | every tick | Output is on while at least one entity matches the selector | `@e[type=minecraft:pig,distance=..5]` |
| Tag Adder | when it gets powered | Adds `tag` to every matching entity | `@e[type=pig,distance=..5]`, tag `custom_tag` |
| Tag Remover | when it gets powered | Removes `tag` from every matching entity — or **all** of their tags if `tag` is empty | `@e[distance=..3]`, tag `custom_tag` |

**Detector output:** put **iron blocks** next to the Entity Detector (anywhere in the 3×3×3 cube
around it). They turn into **redstone blocks** while it detects something.

Tag Adder and Tag Remover act once per power-on (a button press or a pulse), not continuously.
The Entity Detector never counts Redstone Additions' own machines.

!!! tip "Keep selectors small"
    A selector with a `distance` or `type` filter is cheap. A bare `@e` checks every entity in
    the world every tick.

## Ideas

- A pressure plate into a Tag Adder marks players who entered a zone; a Tag Remover at the exit
  clears it.
- An Entity Detector set to `@e[type=item,distance=..2]` turns on when something is dropped on it.

---

## Advanced

- Properties on the marker: `data.properties.entity_selector`, `data.properties.tag`.
- The selector is pasted into a macro command (`ra_sensors:blocks/entity_detector/detect_macro`),
  so it must be a valid selector string.
- Tag Adder / Remover fire on a rising edge: `ra.powered` without `ra.was_powered`, from
  `ra_lib:redstone/detect`.
