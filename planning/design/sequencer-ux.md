# Design note: Sequencer UX (#128)

> **Sequencer**: steps through up to 15 outputs, one per pulse, with a reset input.
> (planning/v6.0.3-ideas.md, Logic Gates)
>
> **Note Sequencer**: like the Sequencer, but a 2×2 matrix so it can play chords.
> (planning/v6.0.3-ideas.md, Audio)

Status: proposal. Nothing here is built yet; the rework waits for the maintainer's choice.

## What ships today (#55, PR #113)

`ra/modules/gates/Sequencer.kt`, documented in docs/logic-gates.md § Sequencer:

- A plain smooth quartz cube with no facing (`dirType = 0`), like every other gate.
- **Input:** a rising edge on the bottom or any of the four sides adds one to `step`
  (1 … `steps`, then back to 1). **Reset:** a rising edge on the **top** sets `step` to 0; while
  the top stays on, the input is ignored (the Counter's convention).
- **Output:** `step` as an analog level on every **target block** in the 3×3×3 cube
  (`analogShell` → `targetPowerFill(1, level)`). **Iron blocks** in the cube turn into redstone
  blocks on the **last** step only (the carry).
- `steps` (1–15, default 4) is cycled by Shift+RMB with the Wrench (15 wraps to 2); `step` is
  readonly. The Goggles show `Step:` and `Steps:`.
- Cost: every tick each marker reads six sides (`any_side`, stops at the first source); the
  shell is written on an edge only, plus a full 3×3×3 `fill` of every Sequencer every 40 ticks
  (the resync that picks up target blocks placed since).

## What is confusing

The plan says "steps through up to 15 **outputs**"; the block delivers **one** output carrying a
number. Playtesting found:

1. **The analog level needs decoding.** To light lamp 3 on step 3 the player must build a
   Signal Calculator set to `equal` with a constant 3 on its top, or a subtract-mode comparator
   pair, per step. Eight steps means eight decoders and eight constant sources. The Sequencer
   is the easy part of the build; the decoding is the build. Dust also loses one level per block,
   so a level-3 signal carried 4 blocks is gone and a level-9 one carried 2 blocks reads as 7;
   the decoders have to sit right against the target block.
2. **Only the last step gets the iron output.** A player who places iron blocks (the convention
   of every other gate) sees nothing for steps 1 … N−1 and concludes the block is broken.
3. **Reset on the top** is invisible: nothing on the block says which face is which. It matches
   the Counter, but a player who powers the Sequencer from above with a button sees it never
   step.
4. **The Wrench changes the step count**, a hidden number. With an analog output there is no
   physical clue that the count is 4; the player has to read the Goggles or chat.
5. **No facing.** "Step 1, 2, 3 …" has no direction in the world, so there is nothing to look at
   that shows the sequence moving.
6. (Minor) The only way to see the current step is the Goggles (`Step: 3`); target blocks do
   not glow and a comparator reads the same value from all of them.

What a player expects from the name is a **chaser**: N outputs in a row, one lit at a time,
moving on by one at each pulse. Every option below is judged against that picture.

## Criteria

- **Understanding:** can a player use it from the item lore alone, without decoders?
- **Build space:** footprint of the Sequencer plus its outputs.
- **Per-tick cost:** idle and per edge. Project rule: no per-tick cost while idle beyond what
  every gate already pays (the side reads).
- **Implementation size:** new blocks, new shared code, build checks touched.
- **Migration:** the Sequencer is in `## [v6.0.3] - Unreleased`, so no released world has one.
  Only dev/playtest worlds (DevWorld) do. A cheap fallback is enough; no `ra/migrations` step is
  required for a release.
- **Note Sequencer reuse:** the Audio block is "the Sequencer with a 2×2 matrix to play chords":
  each step must address up to four notes at once.

## Options

### 1. Numbered "Sequencer Output" companion blocks

A second custom block, *Sequencer Output*, with a `step` property (1–15, Wrench-cycled) and
optionally a `channel`. It lights its own iron shell (redstone block) while its Sequencer is on
its step. Linked by being within N blocks of the Sequencer, or by channel as the Wireless
Emitter/Receiver do (`ra/modules/wireless/Channel.kt`).

- Understanding: good once linked; each output says "Step 3" in the Goggles. Linking is a new
  concept (range or channel) and a mis-set number is invisible.
- Build space: free placement, outputs can sit anywhere, which suits big builds.
- Cost: per edge, `as @e[type=marker,tag=ra.custom_block.sequencer_output,distance=..N]` plus a
  score/property compare per output (or a channel lookup). Idle: each output is a marker in the
  block tick sweep (break test) even though it never reads redstone.
- Implementation: large. New block with recipe, item, Goggles, wrench row, refunds, uninstall,
  docs; a link scheme; two markers to keep consistent (output broken, Sequencer broken).
- Migration: none needed; existing Sequencers keep their analog shell and gain outputs.
- Note Sequencer: workable (four outputs with the same step = a chord), but a chord is then four
  extra blocks per step, each with a number to set by hand.

### 2. Facing Sequencer, outputs on a line of blocks ("chaser")

The Sequencer gets a facing, placed like the Light Sensor (`dirType = 3`, the marker keeps
`data.status.facing`). The **track** is the straight line of blocks leaving its output face:
position k on the track is step k. A track block is an **iron block** (step off) or a **redstone
block** (step on); a **target block** on the track gets `power=15` on its step. On each edge the
previous position goes back to iron and position k turns into a redstone block: dust, a lamp, a
piston or a note block next to block k fires on step k.

```
 input ──> [SEQ] [Fe] [Fe] [RB] [Fe] [Fe] ·   (step 3 of 5; the · ends the track)
            top = reset    1    2    3    4    5
```

- **`steps` comes from the track.** The track is the run of iron / redstone / target blocks in
  front, up to 15, ending at the first other block. Five iron blocks = five steps. The Wrench no
  longer edits a hidden number; the count is what the player built. (The `steps` property stays,
  readonly, shown by the Goggles and the Data Handler.)
- Understanding: very high. It is the chaser players picture, the lit block is visible from a
  distance, and the carry is simply the last block.
- Build space: one block wide, up to 16 long. Outputs are adjacent, so each step's wire leaves
  the track sideways, up or down; alternating sides (or a spacer, see below) keeps them apart.
  Dust beside block k touches only block k. Vertical tracks work too (full facing).
- Cost: idle unchanged (the six side reads become five: the front is the track). Per edge: one
  scan of at most 15 `execute if block` tests to measure the track, then two `setblock`s (old
  position off, new on). The 40-tick 3×3×3 resync fill goes; a resync only needs to rewrite the
  lit position, if kept at all.
- Implementation: small to medium, all inside `Sequencer.kt`: placement stores the facing and
  rotates the marker so `^ ^ ^k` addresses the track, an unrolled `track_length` function, and
  `advance`/`reset`/`output` writing position k instead of the shell. No new block, no new shared
  code (the facing placement is the Light Sensor's).
- Migration: a marker without `data.status.facing` (placed before the rework) keeps the current
  analog behaviour, or is simply broken and placed again in DevWorld. No release migration.
- Note Sequencer: excellent. The track generalises to a track of **slices**: step k is the 2×2
  cross-section at distance k (the block on the axis plus the three to its right, above and
  above-right, or whichever 2×2 layout is chosen). Every note block in slice k plays: up to four
  notes = a chord. Same facing, same scan, same "the build is the score" idea; empty cells are
  rests.

Optional knob (not needed for v1): a `spacing` property (1 or 2) so outputs sit every other
block with a free block between, for players who want dust on both sides.

### 3. One lit output moving around the 3×3 shell

Keep the cube; the lit redstone block walks around the ring of 8 blocks around the Sequencer's
middle layer (or the 26 cube cells).

- Understanding: medium. The movement is visible, but the order (where is step 1? clockwise
  from which face?) needs a facing anyway, and corners only touch dust diagonally.
- Build space: compact (3×3), but eight outputs packed against each other make wiring each step
  out hard: dust next to an edge block also touches its neighbours' positions.
- Cost: as option 2 per edge, no scan.
- Implementation: small, but the order table is a new thing to document.
- Limit: 8 steps in a ring (the plan says up to 15); 26 cube positions have no readable order.
- Note Sequencer: poor; no natural place for four notes per step.

### 4. Keep analog, add a decoder and better explanations

Keep the analog shell and fix the confusion with words and a helper: a *Step Decoder* (or a
Signal Calculator mode `is` with its own constant instead of B) that lights its iron shell
while the incoming level equals its own `level` property; clearer lore ("Outputs the step number
as a signal strength; use a Step Decoder per step"), Goggles line "Output level: 3", docs with a
worked example.

- Understanding: better than today, still two concepts (level, decoder) and still N decoders
  for N outputs. The dust-decay pitfall stays (decoders must touch the target block).
- Build space: a decoder per step, all packed around the target blocks.
- Cost: each decoder is an analog reader every tick (resolving a level is dearer than
  `any_side`), so an 8-step build pays 8 readers forever.
- Implementation: medium (a new block, or a new Calculator operation with a property).
- Migration: none.
- Note Sequencer: poor; four decoders per step for a chord.

The analog output is still worth keeping **as a secondary output**: one dust line carrying the
state into a comparator chain is genuinely useful to advanced players. Options 2 and 4 are not
exclusive.

### 5. Option 2 plus the analog shell (recommended shape)

The facing track of option 2 is the main output. The **target blocks** in the 3×3×3 cube keep the
analog step level, unchanged, for players who want a single wire carrying the step. The iron
part of the shell (lit on the last step only) is dropped, because the last track block already
is the carry and the cube's front cell is the first track position (two rules on one block would
fight).

Two inputs, clearly separated: **back and sides** (and bottom) step, **top** resets, **front** is
the track. The lore names them.

## How the player sees the current step

- **The lit block on the track** (options 2/5): a redstone block among iron blocks, visible at a
  glance, no tool needed. This is the main indicator.
- **Goggles:** `Step: 3 / 5`, `Facing: north`, `Reset: top`. While the Goggles draw the readout
  (only for markers within the Goggles range of a wearer, so no idle cost for players without
  them), a `dust` particle can mark the end of the track, so the player sees where the scan
  stopped when an output block is missing. Particles stay behind the per-player effects setting.
- **Placement feedback:** on placement, one particle line along the measured track, then the
  short chat line "Sequencer: 5 steps" (the same feedback the Wrench gives today).
- **Analog shell** (option 5): a comparator on a target block still shows the level for players
  who want a number.

## Comparison

| | Understanding | Space | Cost per edge | Size | Note Sequencer |
| --- | --- | --- | --- | --- | --- |
| 1 Output blocks | good, linking to learn | free | selector over outputs | large (new block) | 4 blocks per chord |
| 2 Facing track | very good | 1×16 line | ≤15 tests + 2 setblocks | small–medium | 2×2 slices, natural |
| 3 Shell ring | medium | 3×3 | 2 setblocks | small | poor |
| 4 Analog + decoder | fair | decoders packed | decoders read every tick | medium | poor |
| 5 Track + analog | very good | 1×16 line | as 2, plus the target fill | medium | as 2 |

## Recommendation

**Option 5**: a facing Sequencer whose output is the track of iron blocks in front of it (step k
lights the k-th block, the track length is the step count), keeping the analog target-block
level as a secondary output. It turns the block into the chaser the name promises, needs no new
block, costs nothing more while idle, and gives the Note Sequencer its design for free (a track
of 2×2 slices, each slice a chord).

## Implementation sketch

All in `ra/modules/gates/Sequencer.kt` unless noted; follows the Light Sensor's facing pattern.

1. **Facing.** `CustomBlock(..., dirType = 3)`. In `handlePlacement`, copy `ra:temp facing_name`
   into `data.status.facing` (as `LightSensor`), and set the marker's `Rotation` from the facing
   (north 180/0, south 0/0, west 90/0, east −90/0, up 0/−90, down 0/90) so `^ ^ ^k` is the track.
   The model stays smooth quartz; the Goggles show the facing.
2. **Sides.** `INPUT_SIDES` = back, the two sides and the bottom, resolved from the facing (or:
   all sides except top and front; the front is ignored anyway because `IGNORE_BLOCKS` skips the
   redstone block at position 1). Top stays the reset.
3. **`track_length`** (new function): 15 unrolled lines,
   `execute positioned ^ ^ ^k unless block ~ ~ ~ #ra_gates:sequencer_track run return <k-1>`
   with a block tag `ra_gates:sequencer_track` = iron_block, redstone_block, target. Stores the
   result into `data.properties.steps` (now readonly). Called on placement and in `advance`.
4. **`advance`:** `steps` from `track_length`; `step` + 1, wrap after `steps`; if `steps` is 0, do
   nothing. Then `output`.
5. **`output`** (macro on the old and new step): `execute positioned ^ ^ ^<old>` set redstone
   block back to iron, target back to `power=0`; `execute positioned ^ ^ ^<new>` iron → redstone
   block, target → `power=15`. Keep the old step in `data.status.lit` so a step that changed
   through the Data Handler or a wrap still clears the right block. Then `analogShell(N)` for the
   cube's targets, as today. Drop the iron-shell `fill` and the `LIT`/`LAST` tags.
6. **`reset`:** clear the lit position, `step` = 0, analog level 0.
7. **Wrench:** the Steps row goes (the track sets it). Replace with nothing, or with a
   "show track" action that draws the particle line; update the wrench-row build check list.
8. **Break:** `analogOnBreak` plus clearing the lit track position, so a broken Sequencer does not
   leave a redstone block standing.
9. **Resync:** drop the 40-tick cube fill; optionally re-run `output` on the 40-tick tick only
   for markers whose lit position is no longer a redstone block.
10. **Fallback for DevWorld:** a marker without `data.status.facing` skips steps 3–5 and keeps the
    current analog output, or the docs ask to replace it. No `ra/migrations` step: the block is
    unreleased.
11. **Docs and texts:** item lore ("Each pulse lights the next block of the iron line in front of
    it. Top: reset."), Goggles `Step: k / n`, `Facing:`; docs/logic-gates.md § Sequencer
    rewritten around a chaser example with a diagram; changelog entry under v6.0.3 rewritten
    (still Unreleased, so edit the existing **Sequencer** entry rather than adding a "Changed").
12. **Note Sequencer later:** extract `facing + track_length + positioned ^ ^ ^k` into a small
    helper in `GateCommon.kt` (or a shared `TrackOutput`) when the Audio block needs it; the slice
    is the same position plus `^1 ^ ^`, `^ ^1 ^`, `^1 ^1 ^`.
