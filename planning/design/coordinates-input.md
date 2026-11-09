# Design note: entering coordinates (#48)

> A simple way to enter relative, look-relative (`^`) and absolute coordinates for blocks that
> need a position. (planning/v6.0.3-ideas.md, Rules and UI)

Status: proposal. Nothing here is built yet; implementation waits for the maintainer's choice.

## The problem

Some blocks act on a position other than their own. Today the only way to give one is to type it
as text in the Data Handler's book, and only one kind of block does it: the Boxer and Unboxer, whose
`input1` / `output1` are `TextFormat.COORDS` text such as `^ ^ ^-1`.

That works, but:

- **Typing is awkward.** `^` and `~` are dead keys or hidden behind AltGr on many keyboards, the
  format is strict (one space between parts, nothing else), and a player has to know what `^`
  means.
- **The range is tiny.** The `COORDS` checker accepts at most one whole digit per part, so 9 blocks
  is the hard limit. The plan's Block Detector will want more, and admins are meant to bypass range
  limits ("Admins bypass range limits", same plan).
- **No absolute coordinates.** `COORDS` refuses them on purpose: digits without `~`/`^` are three
  unchecked numbers of any size.
- **Mixing `~` and `^` passes the check.** `~ ~ ^1` is accepted by `ra_lib:safe/check` but is not a
  valid position, so the macro line fails at run time. It cannot run anything else (no letters, no
  spaces past the third part), but the block silently does nothing.
- **Safety rests on walking text.** The value is pasted into `execute positioned $(input1)`, so
  every sink has to call `guardProperty` first (see "A property pasted into a macro is part of the
  command" in docs/gotchas.md). It is safe today, but each new block has to remember it.

### Which blocks need a position

| Block | Status | What the position is | Needs `^`? |
| --- | --- | --- | --- |
| Boxer, Unboxer | shipped | input and output containers | yes: "behind me", "in front of me" survive the Clipboard onto a block facing another way |
| Block Detector | planned (Sensors) | the block to watch | useful; absolute is useful too ("watch the door at 120 64 -30") |
| Block Breaker / Placer | shipped, fixed to the block in front | could gain a target offset later | yes |
| Fan | planned | a **range**, not a position | no: a number property (`number("range", …)`) is enough |
| Anchor Terminal / Tablet | planned (Ender Links) | a **destination name** from the anchor table | no: typed coordinates would be a free teleport; destinations stay placed anchors |
| Sprinkler, Tesla Coil | planned | a radius | no: a number |

So the real users are the Boxer/Unboxer and the Block Detector, with the Breaker/Placer as a
likely later one. The Fan, anchors and radius blocks should **not** use this; a number or a name is
the right shape for them.

A useful observation: for a block that never turns after placement, `^` and `~` reach the same
block. `^` only matters when the setting is copied (Clipboard) or the block can be re-faced
(Wrench). That is why it is still worth offering, but it does not need to be the default.

## Part 1: how the value is stored

### Option S1: keep a text property (`TextFormat.COORDS`), widen the grammar

Allow more digits, allow plain numbers for absolute, reject mixed `~`/`^`.

- Player experience: unchanged (still typing).
- Cost: the walk is per character, cached by `guard` while unchanged; fine.
- Complexity: the checker grows (absolute needs a range check, which a character walk cannot do:
  it would have to parse the number into a score).
- Safety: still "text pasted into a command", guarded by a walker. Every new sink must remember
  `guardProperty`. One mistake in the grammar is an injection.

### Option S2: three integers and a mode (recommended)

A new property shape, say `pos("target", max = 16)`, stored as
`{x:<int>, y:<int>, z:<int>, m:<byte>}` where `m` is 0 relative (`~`), 1 local (`^`), 2 absolute.

The sink never pastes the stored value. It copies each part through a score, which can only ever
produce an integer, then pastes **that**:

```mcfunction
execute store result storage ra:temp pos.x int 1 run data get entity @s data.properties.target.x
execute store result storage ra:temp pos.y int 1 run data get entity @s data.properties.target.y
execute store result storage ra:temp pos.z int 1 run data get entity @s data.properties.target.z
execute if data entity @s data.properties.target{m:0b} run return run function ns:at_rel with storage ra:temp pos
execute if data entity @s data.properties.target{m:1b} run return run function ns:at_local with storage ra:temp pos
execute if data entity @s data.properties.target{m:2b} run return run function ns:at_abs with storage ra:temp pos
# at_rel:   $execute positioned ~$(x) ~$(y) ~$(z) run ...
# at_local: $execute positioned ^$(x) ^$(y) ^$(z) run ...
# at_abs:   $execute positioned $(x) $(y) $(z) run ...
```

- Player experience: enables every input method below; the Goggles and the Data Handler can show
  it nicely (`3 up, 2 in front`, or `120 64 -30`).
- Cost: three `data get` and one macro call per use, less than a text walk. Only on a run that
  needs the position (a powered tick, a detection), never idle.
- Complexity: a new `PropType` (or a compound built from three `number` specs plus a mode) in
  `core/Properties.kt`, a Data Handler row for it, Clipboard and Goggles support. The mode is never
  pasted: it selects one of three fixed functions.
- Safety: **safe by construction.** Whatever is in the compound (a string from the Creative Data
  Handler, an old world, a `/data` command), `store result … int` turns it into an integer or into
  0. Nothing typed by a player reaches a command as text. No `guardProperty` to forget.
- Range: checked where the value is accepted (Data Handler, pick, dialog) against the block's
  maximum, skipped for admins as the plan asks. The sink also clamps relative and local parts to the
  server maximum (a GLOBAL setting) unless the value carries `a:1b` (set by an admin); the Clipboard
  drops `a` when a non-admin pastes, so copying an admin's value cannot carry the bypass.

### Option S3: a hidden target marker

Summon a marker at the chosen position and act `at` it.

- Player experience: good (nothing to type at all).
- Cost: one more entity per block; its chunk can be unloaded while the block's is loaded.
- Complexity: lifecycle (break, purge epoch stamping, migrations, Clipboard cannot copy it).
- Safety: nothing pasted, fully safe.
- Rejected: too much lifecycle for what three integers do.

## Part 2: how the player enters it

### Option E1: typing in the Data Handler book (today)

- Player experience: poor for `^`/`~`; fine for people who know commands.
- Cost / complexity: exists.
- Safety: with S2, the text is never stored: it would have to be parsed into three integers,
  which is hard in commands. Keep it only as the format of the existing Boxer values.

### Option E2: three number prompts through `/trigger`

The existing number backend (`ra_lib:input`, trigger) asks for X, then Y, then Z, then the mode
with `open_choice`.

- Player experience: four rounds of chat; slow, but works for any range and for absolute values.
- Cost: none idle (the input session already exists).
- Complexity: low; reuses `InputRange` for bounds.
- Safety: `/trigger` only takes an integer and only changes the player's own trigger score; there
  is nothing to extend.

### Option E3: a dialog (26.3 `/dialog`)

The Data Handler's **[Modify]** on a position row opens a dialog: a mode switch
(`single_option`: Relative / In front / Absolute) and three sliders (`number_range`). Its submit
button is a `dynamic/run_command` whose template is a `/trigger`, which needs no permission and no
confirmation screen (`settings/Menu.kt` explains why every RA chat button is a trigger).

One submit can run only one command, and a trigger takes one integer, so the four inputs are packed
into one number. Each slider runs over `10..88`, meaning an offset of `-39..39` (value − 49), and
always prints two digits:

```mcfunction
dialog show @s {type:"minecraft:confirmation",title:"Target",inputs:[
  {type:"minecraft:single_option",key:"m",label:"Mode",options:[{id:"1",display:"Relative (~)"},{id:"2",display:"In front (^)"}]},
  {type:"minecraft:number_range",key:"x",label:"X",start:10,end:88,step:1,initial:49}, ...y, z...],
  yes:{label:"Set",action:{type:"minecraft:dynamic/run_command",template:"trigger ra.input.trigger set $(m)$(x)$(y)$(z)"}},
  no:{label:"Cancel"}}
```

`1 52 49 47` arrives as `1524947`: mode 1, x +3, y 0, z −2. The pack decodes it with `/` and `%`
on scores. (Validated with `validate_command` for 26.3; the inline dialog can be opened through a
macro whose only variables are the current values as integers, so the sliders start where the
block is.)

- Player experience: the best for offsets. One screen, sliders, a clear mode switch, no special
  keys. Works on every keyboard.
- Cost: nothing idle; one dialog per edit.
- Complexity: medium. A packed-number decoder, and one dialog shape shared by every position row.
  To confirm while building: that `number_range` writes a whole step as `52` and not `52.0` into the
  template. If it does not, the sliders become text inputs (one value per submit, chained with
  `after_action: wait_for_response`).
- Safety: the only thing that reaches the server is `/trigger <int>`. A slider cannot produce a
  space or a letter; even text typed into a text input would just make `/trigger` fail to parse.
- Limits: ±39 per axis by sliders, which covers every planned block. Absolute values do not fit in
  a slider, see E4.

### Option E4: pointing at a block (pick)

A **[Pick]** button on the position row arms the player for 30 seconds; their next right-click on
a block with the Data Handler (the same raycast the tools already use) stores that block.

- Relative / local mode: the pack stores the offset from the configured block (for `^`, converted
  with the block's facing; four or six fixed cases generated in Kotlin, no macro).
- Absolute mode: the pack stores the clicked block's world coordinates.
- Player experience: the most natural for "watch that block" (the Block Detector). Limited by reach
  (the raycast's few blocks), so far targets still need E3 or E2.
- Cost: one tag check per tool use while armed; nothing idle.
- Complexity: medium; the Data Handler already keeps a selected block per player (`ra.dh.sel`),
  so the pick reuses it.
- Safety: the values come from the game (`store result` of the raycast position), never from text.

### Option E5: Wrench clicks (first click the block, then the target)

Like E4 but with the Wrench.

- Rejected: the Wrench's job is cycling settings and assembling; a second selection mode on it
  conflicts with Shift + right-click, and the Data Handler is where settings are edited.

## Recommendation

1. **Store positions as S2** (three integers plus a mode byte), through a new property shape in
   `core/Properties.kt`. Sinks paste only integers copied through scores and pick one of three fixed
   macro functions by mode. This makes coordinates impossible to turn into a command, without a
   text walker.
2. **Enter them in the Data Handler with E3 + E4**: **[Modify]** opens the dialog (mode and three
   sliders, ±39), **[Pick]** takes a looked-at block (and is the way to set an absolute position);
   E2 (three trigger prompts) stays as the fallback, also used for absolute values typed by hand.
3. **Do not use it** for the Fan, radius blocks or the anchors: they get numbers and names.
4. **Boxer / Unboxer**: leave their `COORDS` text as is in v6.0.3 (it is safe and shipped). Move
   them to S2 later with a migration in `ra/migrations` that converts the common values
   (`^ ^ ^-1`, `^ ^ ^1`, `^ ^1 ^`, …) through a fixed table and leaves any other value as text with
   its guard.
5. Separately, reject mixed `~`/`^` in `ra_lib:safe/check`'s `COORDS` walk, so a bad value is
   refused when typed instead of failing silently.

### Implementation sketch

- `core/Properties.kt`: `fun pos(name, max: Int = 9, modes = setOf(REL, LOCAL, ABS), default = …)`
  writing a schema row with `type:"pos"`, `max`, the allowed modes. Docs: tools-and-registries.md.
- `ra/lib/pos/` (new, small): `at {p, run}` (the three `store result` lines and the three fixed
  macro functions), `decode` (packed trigger value → x, y, z, m), `to_local/<facing>` (world offset
  → `^` offset).
- Data Handler: a row renderer for `pos` (`3 up, 2 in front` / `120 64 -30`, the mode in colour),
  buttons **[Modify]** (dialog), **[Pick]**, **[Type]** (three trigger prompts). The range check
  uses the schema `max` and skips it for `ra.admin`.
- Clipboard: copies the compound; drops `a:1b` when the paster is not an admin.
- Goggles: while a player holds the Data Handler with a block selected, a few `dust` particles at
  the target, shown to that player only (`particle … @s`), respecting `ra.u.par`.
- First user: the Block Detector (`pos("target", max = 16)`, default `^ ^ ^1`).
