# Design note: telling which structure is being built (#78)

> Open problem: it is hard for a player to tell which structure they are building.
> (planning/v6.0.3-ideas.md, Multiblocks)

Status: proposal. Nothing here is built yet; implementation waits for the maintainer's choice.

## The problem

A multiblock starts as a bare **Multiblock Base** of some tier. The player builds blocks around it
and right-clicks the base with the Wrench. Until that click succeeds, nothing in the world says
which machine the blocks belong to or how far along it is.

What exists today:

- **The wrench's failure message** (`tools/wrench/WrenchMultiblock.kt`): "No copper-tier multiblock
  matches the structure here", followed by one hard-coded hint line per structure of that tier.
  The lines are written in the wrench, not taken from each spec's `hint`, so an add-on's
  structure or a changed spec is not described, and the message does not say which structure is
  closest or which block is wrong.
- **The debug report** (`ra_lib_multiblock:spec/<id>/debug/<facing>`): lists every wrong block and
  what belongs there, then `19/26 blocks OK`. It is exactly what a player needs, but it is for
  operators: it needs the `ra.debug` tag, a command, the structure id and the facing.
- **docs/multiblocks.md**: text diagrams per machine. Good, but outside the game.
- **The Goggles** show a base as a plain custom block; they draw nothing for an unassembled
  structure.

The difficulty grows with tiers: one base tier serves several structures (Copper has the Blast
Forge and the Rock Metallic Drill), structures rotate (four facings), and add-ons can register
more. (The Upgrade Platform, the only Netherite structure, is being removed in v6.0.3.)

### What the game gives us

- Every compiled spec is known in Kotlin with its blocks already rotated for each facing
  (`spec.blocksFacing(facing)`), so per-structure, per-facing functions can be generated with
  no macros, exactly like `debugBlocks` in `multiblocklib/spec/Compiled.kt` already does.
- Counting matching blocks is one `execute if block` per block: about 26 checks for the Blast
  Forge per facing, about 100 for all four facings.
- Storage-registered (add-on) structures only exist at run time; they can be counted with the
  library's macro walk, more slowly.

## Options

### O1: a better wrench message (closest structure and progress)

On a failed assembly, count each structure of the base's tier in each facing and report the best:
`Closest: Blast Forge (facing north), 19/26 blocks. 7 wrong or missing.`, then the spec's `hint`,
then a clickable **[Show missing]** (see O2/O3) and the other structures of that tier as
**[Show Rock Metallic Drill]** buttons.

- Player experience: much better than today, on the action the player already does. Still only
  after they think they are done.
- Cost: on click only. ~100 block checks per structure of the tier; nothing idle.
- Complexity: low. A generated `spec/<id>/count/<facing>` per spec (the debug lines without the
  `tellraw`), and `try_tier` keeping the best score. Hints come from the specs, so the wrench's
  hard-coded list goes away.
- Safety: no player input; nothing typed.

### O2: ghost blocks (preview displays)

After **[Show missing]** (or a Shift + right-click on an empty base), summon a small
`block_display` of the needed block at every wrong or missing position, scaled down (0.5), glowing,
tagged `ra.mb_ghost`, for 30 seconds or until the structure assembles or the base is broken.

- Player experience: the clearest by far; you see exactly what goes where, and the block type, in
  the world. Works for any structure, including add-ons' (from `facings.<f>.blocks`).
- Cost: up to ~26 display entities for 30 s, on demand. A timer score on the base marker gated by a
  tag, so no idle cost. Displays are seen by everyone nearby (like Goggles billboards); there is no
  per-player entity visibility in vanilla.
- Complexity: medium. Block tags (`#ra_multiblock:blast_forge_bricks`) need a representative block
  for the display (the first entry, chosen in Kotlin); states (`furnace[lit=true]`) need the state
  copied. Clean-up on timeout, assembly, break, uninstall and purge (orphan displays are already
  handled by the purge's "no marker within 0.6" rule only if the ghosts sit next to the base, which
  they do not: they need their own tag and kill path).
- Facing: the best facing from O1; if nothing is built yet, the player's look direction.

### O3: particles on the missing blocks

Same as O2 but with `dust` particles at each missing position, every 10 ticks for 30 s, shown only
to the player who asked (`particle … @s`).

- Player experience: shows where, not what (a particle cannot show a block type). Respects the
  per-player `ra.u.par` setting, which is also its weakness: with particles off it shows nothing.
- Cost: a short per-player session, gated by a tag; nothing idle.
- Complexity: low-medium.
- Safety: none involved.

### O4: Goggles hint on an unassembled base

The Goggles' label on a base shows `Closest: Blast Forge 19/26`.

- Player experience: good and passive, progress updates as you build.
- Cost: the count runs on every Goggles refresh (once a second by default) for each unassembled
  base in range of a wearer: ~100 to ~200 block checks per base per second, only while worn. Fine
  for a few bases, noticeable for a builder with many.
- Complexity: low once O1's count exists (a readout for the base block).

### O5: choosing the structure first

Shift + right-click a base with the Wrench to cycle a `target` property (Blast Forge / Rock
Metallic Drill / any); the base then only checks and shows that structure.

- Player experience: removes the guessing completely; makes O2/O4 cheaper and unambiguous. One
  more step, but optional (`any` keeps today's behaviour).
- Cost: lowers every other option's cost (one structure instead of the whole tier).
- Complexity: low; a `list`-style wrench setting (the Wrench menu already cycles settings), its
  values built from the tier's specs at load.
- Safety: a cycled value, never typed.

### O6: a guide item or book

A **Multiblock Manual** (written book or dialog) listing each structure with a layer-by-layer
diagram and the base tier.

- Player experience: helps before building; does not help locate a wrong block.
- Cost: none at run time.
- Complexity: diagrams in chat/book text are hard to read for 3x3x3 shapes; a dialog with `item`
  body elements per layer is better but long to author. Must be generated from specs to stay in
  step.

### O7: chat progress while building

Tell the player `Blast Forge 20/26` each time they place a block near a base.

- Rejected: needs a placement hook for every vanilla block near any base (an advancement on
  `placed_block` plus a search for a nearby base), and is chatty. O4 gives the same information
  without spam.

## Recommendation

Do **O1 + O5 + O2** in v6.0.3, add **O4** if the count proves cheap in practice, leave O6 for
later.

1. **O1**: the wrench's failure message names the closest structure, its facing and `n/total`,
   uses each spec's own `hint`, and offers **[Show missing]** plus one **[Show …]** per other
   structure of the tier. The hard-coded hint lists in `WrenchMultiblock.kt` go.
2. **O5**: an optional `target` on the base (Wrench cycle, default `any`) so a player can say what
   they are building; every check then looks at that one structure.
3. **O2**: **[Show missing]** draws ghost blocks for 30 seconds at the wrong or missing positions,
   in the best facing (or the player's facing on an empty base). Ghosts disappear on assembly,
   on break, on timeout and on uninstall/purge.
4. **O4**: the Goggles label on an unassembled base reads `Closest: <name> n/total` (or the
   `target`'s progress), computed only during the Goggles refresh.

### Implementation sketch

- `multiblocklib/spec/Compiled.kt`: generate `spec/<id>/count/<facing>` (one
  `execute if block … run scoreboard players add #mb_count ra.temp 1` per block, then
  `return run scoreboard players get #mb_count ra.temp`) next to `debug/<facing>`, and
  `spec/<id>/ghost/<facing>` (one `execute unless block … run summon block_display …` per block,
  the representative block and states resolved in Kotlin).
- `multiblocklib/Closest.kt` (new): `closest {tier}` tries every compiled spec of the tier and each
  facing, keeps the best in `storage ra:multiblock closest {id,name,facing,ok,total}`; storage
  registered types go through the macro walk after them.
- `tools/wrench/WrenchMultiblock.kt`: on failure, `closest` and print the result with the
  buttons; the buttons are `/trigger ra.wrench` actions (no permission needed), carrying the
  structure and facing as an action code.
- `modules/multiblock/blocks/Bases.kt`: `target` property (a cycled list, default `any`), a ghost
  timer score on the base marker, its kill on break; uninstall and purge kill `ra.mb_ghost`.
- Goggles: a base readout calling `closest` (or the target's `count`) during the refresh.
- docs/multiblocks.md "Building a machine" and docs/multiblock-library.md (the new generated
  functions) updated alongside.
