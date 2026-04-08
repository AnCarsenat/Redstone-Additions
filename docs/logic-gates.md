# Logic Gates

The `ra_gates` namespace provides compact logic and timing blocks for vanilla redstone builds.

- Namespace: `ra_gates`
- Give all: `/function ra_gates:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Summary

| Block | Item model | Recipe | Main property |
|---|---|---|---|
| UNI Gate | `minecraft:smooth_stone_slab` | ![UNI Gate recipe](images/recipes/ra_gates/uni_gate.png){ width="220" } | `gate_type` |
| Clock | `minecraft:daylight_detector` | ![Clock recipe](images/recipes/ra_gates/clock.png){ width="220" } | `cooldown` |
| Delayer | `minecraft:quartz_block` | ![Delayer recipe](images/recipes/ra_gates/delayer.png){ width="220" } | `delay` |
| Extender | `minecraft:quartz_block` | ![Extender recipe](images/recipes/ra_gates/extender.png){ width="220" } | `extend` |
| Randomizer | `minecraft:bookshelf` | ![Randomizer recipe](images/recipes/ra_gates/randomizer.png){ width="220" } | `chance` |
| Shortener | `minecraft:quartz_block` | ![Shortener recipe](images/recipes/ra_gates/shortener.png){ width="220" } | `pulse` |

## Shared Signal Model

Gate processing runs `ra_lib:redstone/detect` directly inside each block process function.

- Inputs are read from `ra.power.north/south/east/west/up/down` (`0..16`).
- Active input count is computed as the number of directions with power `>= 1`.
- Legacy-like inactive input handling for `and`/`nand` parity checks adjacent `redstone_wire[power=0]` and `lever[powered=false]`.
- Any power in `1..16` counts as active for gate truth evaluation.

Each gate then resolves output behavior from those derived input states and local properties.

## Per-Block Behavior

## UNI Gate

- Seven modes: `and`, `or`, `not`, `xor`, `nand`, `nor`, `xnor`
- Mode value in `data.properties.gate_type`
- Wrench cycles through available modes

## Clock

- Periodic pulse generator
- Property: `data.properties.cooldown`
- Includes migration compatibility for older `delay` key usage

## Delayer

- Delays rising signal by configurable ticks
- Property: `data.properties.delay`
- Default value: 20 ticks

## Extender

- Extends output after input turns off
- Property: `data.properties.extend`
- Default value: 20 ticks

## Randomizer

- Random chance output on rising edge
- Property: `data.properties.chance`
- Effective range: 0 to 100

## Shortener

- Converts input into fixed-width pulse
- Property: `data.properties.pulse`
- Default value: 2 ticks

## Debug and Tuning

1. Use CDH to tune gate properties live.
2. Use goggles to inspect runtime state while the circuit is active.
3. If behavior appears inconsistent, check for orphan markers near replaced gates.

---
