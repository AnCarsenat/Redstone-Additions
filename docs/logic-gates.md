# Logic Gates

Compact logic and timing blocks for redstone builds: one universal gate that does AND, OR, NOT
and the rest, plus a clock, a delayer, a pulse extender, a pulse shortener, a randomizer, a
counter and an edge detector, and two analog blocks that work with signal strengths: a signal
calculator and a sequencer.

## Getting them

| Block | Recipe |
| --- | --- |
| UNI Gate | ![UNI Gate recipe](images/recipes/ra_gates/uni_gate.png){ width="220" } |
| Clock | ![Clock recipe](images/recipes/ra_gates/clock.png){ width="220" } |
| Delayer | ![Delayer recipe](images/recipes/ra_gates/delayer.png){ width="220" } |
| Extender | ![Extender recipe](images/recipes/ra_gates/extender.png){ width="220" } |
| Randomizer | ![Randomizer recipe](images/recipes/ra_gates/randomizer.png){ width="220" } |
| Shortener | ![Shortener recipe](images/recipes/ra_gates/shortener.png){ width="220" } |
| Counter | ![Counter recipe](images/recipes/ra_gates/counter.png){ width="220" } |
| Edge Detector | ![Edge Detector recipe](images/recipes/ra_gates/edge_detector.png){ width="220" } |
| Signal Calculator | ![Signal Calculator recipe](images/recipes/ra_gates/signal_calculator.png){ width="220" } |
| Sequencer | ![Sequencer recipe](images/recipes/ra_gates/sequencer.png){ width="220" } |

Operators: `/function ra_gates:items/give_all`.

## How they work

**Input:** redstone power on any side of the block — dust, levers, buttons, torches, repeaters,
comparators, pressure plates.

**Output:** put **iron blocks** next to the gate (anywhere in the 3×3×3 cube around it). While the
gate's output is on, those iron blocks turn into **redstone blocks**; when it switches off they
turn back. Wire your circuit from those blocks.

**Analog output (Signal Calculator, Sequencer):** put **target blocks** in the same 3×3×3 cube.
They take the gate's output level, 0 to 15, as their own signal strength: dust next to one
carries that level, and a comparator behind one reads it. Iron blocks still work too, as on/off
(see each block). A [Light Sensor](sensors.md) is itself a target block, so keep it out of the
cube.

## The blocks

| Block | Does | Setting | Default |
| --- | --- | --- | --- |
| UNI Gate | A logic gate: `and`, `or`, `not`, `xor`, `nand`, `nor`, `xnor` | `gate_type` | `and` |
| Clock | Pulses the output for one tick, every `cooldown` ticks, forever | `cooldown` (ticks, at least 2) | 20 |
| Delayer | Turns on `delay` ticks after the input turns on | `delay` (ticks) | 20 |
| Extender | Stays on for `extend` ticks after the input turns off | `extend` (ticks) | 20 |
| Randomizer | Each time the input turns on, turns on with `chance`% probability | `chance` (0–100) | 50 |
| Shortener | Turns any input into a pulse of exactly `pulse` ticks | `pulse` (ticks) | 2 |
| Counter | Counts the times the input turns on; fires when the count reaches `target`. Top side resets | `target` (1–1000), `hold` | 10, off |
| Edge Detector | A pulse of `pulse` ticks when the input turns on, off, or both | `mode` (`rising`, `falling`, `both`), `pulse` (ticks) | `rising`, 2 |
| Signal Calculator | Maths on two signal levels: A from the sides and bottom, B from the top | `operation` | `add` |
| Sequencer | Steps 1, 2, … `steps`, one step per input pulse; the step is its output level. Top side resets | `steps` (1–15) | 4 |

20 ticks = 1 second.

### Counter

- **Counting:** each time redstone on the bottom or any of the four sides turns **on**, the count
  goes up by one. A signal that stays on counts once.
- **Reset:** redstone on the **top** (a lever, button or dust on it, or a powered block above)
  sets the count back to 0 and turns the output off. While the top stays powered, nothing is
  counted.
- **On reaching `target`**, with `hold` off (the default): the output pulses for 2 ticks and the
  count starts again from 0, so a Counter with target 4 turns every 4th pulse into an output
  pulse. With `hold` on: the output turns on and stays on, and the count stays at the target,
  until the top is powered.
- The count is shown by the Goggles and the Data Handler (read only; the Creative Data Handler
  can set it).

### Edge Detector

| Mode | Pulses when… |
| --- | --- |
| `rising` | the input turns on (the same as a Shortener) |
| `falling` | the input turns off |
| `both` | the input turns on or off |

Each pulse lasts `pulse` ticks (1–200). An edge that arrives while a pulse is still on restarts
it, so the output stays on rather than flickering. Any other value typed into `mode` acts as
`rising`.

### Signal Calculator

- **A** is the strongest signal arriving on the bottom or any of the four sides; **B** is the
  signal on the **top** (dust or a lever on it, or a powered block above). Feed levels with
  redstone dust: a repeater or comparator pointing into the block reads as 15.
- The result, 0 to 15, goes to the target blocks around it. Iron blocks around it turn into
  redstone blocks while the result is above 0, so the comparisons also drive plain on/off
  circuits.

| `operation` | Output |
| --- | --- |
| `add` | A + B, at most 15 |
| `subtract` | A − B, at least 0 |
| `max` | the larger of A and B |
| `min` | the smaller of A and B |
| `greater` | 15 when A > B, else 0 |
| `equal` | 15 when A = B, else 0 |
| `less` | 15 when A < B, else 0 |
| `invert` | 15 − A (B is not used) |

Shift + right-click with the Wrench steps through them in this order. Any other value typed into
`operation` acts as `add`. The Goggles show the operation, A, B and the output.

### Sequencer

- **Stepping:** each time redstone on the bottom or any of the four sides turns **on**, the step
  goes up by one: 1, 2, … up to `steps` (1–15, default 4), then back to 1.
- **Reset:** redstone on the **top** sets the step to 0 (no output). While the top stays powered,
  the input is ignored; the next pulse after it is step 1.
- **Output:** the step number is the level on the target blocks around it, so a single dust line
  carries the whole state. To act on one step, decode it: a Signal Calculator set to `equal`
  with that level on its top, or a comparator in subtract mode. Iron blocks around it turn into
  redstone blocks on the **last** step only, which is the carry for chaining Sequencers.
- Shift + right-click with the Wrench adds one step (15 wraps to 2). The step is shown by the
  Goggles and the Data Handler (read only).

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
- **Counter hold, Edge Detector mode, Signal Calculator operation and Sequencer steps:**
  Shift + right-click with the [Wrench](tools.md) too.
- **Timings, chance, the Counter target and the Sequencer steps:** open the block with the [Data Handler](tools.md) and edit the value.
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
  `extend`, `chance`, `pulse`, `target`, `hold`, `count`, `mode`, `operation`, `steps`, `step`). The Clock also accepts an old
  `delay` property and renames it to `cooldown`.
- The Counter and the Edge Detector only write anything when their input changes: a steady input
  costs the same side reads as any other gate and nothing more. The Counter reads its five
  counting sides and its top separately with `ra_lib:redstone/any_side`; its count lives in
  `data.properties.count`, written once per counted edge. Output pulses are timed on the marker's
  `ra.cooldown` score.
- The analog output is `fill … target[power=N] replace target` over the 3×3×3 cube
  (`targetPowerFill` in `core/block/Redstone.kt`, shared with the Light Sensor). A target set to a
  power by a command keeps it as long as it stays a target block; an arrow hitting one pulses it
  as in vanilla until the gate writes its level again.
- The Signal Calculator asks each side `ra_lib:redstone/any_side` first and resolves the level
  (`ra_lib:redstone/side`) only on a powered side, so an unpowered Calculator costs what any other
  gate costs. A and B are folded into one key kept on the marker's `ra.power` score; the result is
  worked out and written only when the key changes, and once every 40 ticks, which picks up an
  operation typed into the Data Handler and a target block placed since. The add, subtract,
  max, min and invert results are one `/compute` each. A, B and the output are kept in
  `data.status` for the Goggles.
- The Sequencer reads its sides as the Counter does and writes only on an edge; every 40 ticks it
  rewrites its output once, for target blocks placed since.
