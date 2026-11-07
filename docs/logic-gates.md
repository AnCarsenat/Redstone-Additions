# Logic Gates

Compact logic and timing blocks for redstone builds: one universal gate that does AND, OR, NOT
and the rest, plus a clock, a delayer, a pulse extender, a pulse shortener and a randomizer.

## Getting them

| Block | Recipe |
| --- | --- |
| UNI Gate | ![UNI Gate recipe](images/recipes/ra_gates/uni_gate.png){ width="220" } |
| Clock | ![Clock recipe](images/recipes/ra_gates/clock.png){ width="220" } |
| Delayer | ![Delayer recipe](images/recipes/ra_gates/delayer.png){ width="220" } |
| Extender | ![Extender recipe](images/recipes/ra_gates/extender.png){ width="220" } |
| Randomizer | ![Randomizer recipe](images/recipes/ra_gates/randomizer.png){ width="220" } |
| Shortener | ![Shortener recipe](images/recipes/ra_gates/shortener.png){ width="220" } |

Operators: `/function ra_gates:items/give_all`.

## How they work

**Input:** redstone power on any side of the block — dust, levers, buttons, torches, repeaters,
comparators, pressure plates.

**Output:** put **iron blocks** next to the gate (anywhere in the 3×3×3 cube around it). While the
gate's output is on, those iron blocks turn into **redstone blocks**; when it switches off they
turn back. Wire your circuit from those blocks.

## The blocks

| Block | Does | Setting | Default |
| --- | --- | --- | --- |
| UNI Gate | A logic gate: `and`, `or`, `not`, `xor`, `nand`, `nor`, `xnor` | `gate_type` | `and` |
| Clock | Pulses the output for one tick, every `cooldown` ticks, forever | `cooldown` (ticks, at least 2) | 20 |
| Delayer | Turns on `delay` ticks after the input turns on | `delay` (ticks) | 20 |
| Extender | Stays on for `extend` ticks after the input turns off | `extend` (ticks) | 20 |
| Randomizer | Each time the input turns on, turns on with `chance`% probability | `chance` (0–100) | 50 |
| Shortener | Turns any input into a pulse of exactly `pulse` ticks | `pulse` (ticks) | 2 |

20 ticks = 1 second.

### UNI Gate modes

| Mode | Output is on when… |
| --- | --- |
| `and` | every connected input is powered |
| `or` | at least one input is powered |
| `not` | no input is powered |
| `xor` | exactly one input is powered |
| `nand` | not every connected input is powered |
| `nor` | no input is powered |
| `xnor` | not exactly one input is powered |

For `and` / `nand`, every redstone component touching the gate counts as an input — an unpowered
repeater on one side keeps an AND gate off.

## Changing settings

- **UNI Gate mode:** Shift + right-click the gate with the [Wrench](tools.md) to cycle modes.
- **Timings and chance:** open the block with the [Data Handler](tools.md) and edit the value.
- **Defaults for newly placed gates:** server operators can change them in
  [Settings](settings.md) → Logic Gates.
- **Check a gate:** wear the [Goggles](tools.md) to see its mode or timing above it.

---

## Advanced

- Each gate reads its inputs with `ra_lib:redstone/detect` every tick; any power level from 1 to
  16 counts as on.
- `and` / `nand` compare the powered sides with the number of sides that carry a redstone source
  at all (`ra_lib:redstone/count_inputs`).
- The output is a `fill … redstone_block replace iron_block` over the 3×3×3 cube, rewritten only
  when the result changes; the UNI Gate re-syncs it every 40 ticks, which also repairs a shell
  edited by hand.
- Properties live on the block's marker in `data.properties` (`gate_type`, `cooldown`, `delay`,
  `extend`, `chance`, `pulse`). The Clock also accepts an old `delay` property and renames it to
  `cooldown`.
