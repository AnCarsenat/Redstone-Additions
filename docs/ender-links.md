# Ender Links

The `ra_ender` module moves things between two places that are nowhere near each
other. Three **vaults** link a pair of endpoints by channel — items, fluids,
power — and a **Teleport Anchor** moves the player.

- Namespace: `ra_ender`
- Give all: `/function ra_ender:items/give_all`
- Anchor help: `/function ra_ender:tools/anchor/help`
- Runtime architecture: [How It Works](how-it-works.md)

## Vaults

| Block | Physical block | Carries | Recipe |
| ----- | -------------- | ------- | ------ |
| Ender Item Vault | `minecraft:barrel` | Item stacks | ![Ender Item Vault recipe](images/recipes/ra_ender/ender_item_vault.png){ width="220" } |
| Ender Fluid Vault | `minecraft:purpur_block` | Liquid and gas | ![Ender Fluid Vault recipe](images/recipes/ra_ender/ender_fluid_vault.png){ width="220" } |
| Ender Power Vault | `minecraft:purpur_pillar` | EU | ![Ender Power Vault recipe](images/recipes/ra_ender/ender_power_vault.png){ width="220" } |

Every vault has two properties that matter:

| Property | Meaning |
| -------- | ------- |
| `channel` | A string. Two vaults with the same channel are linked. Default `"default"` |
| `mode` | `shared` (default), `link`, or `send` / `receive` for a one-way pipe |

Set the channel with the [Data Handler](interactive-machines.md); cycle the mode
with **shift + right-click of the Wrench**. A channel is a pair in practice: a
vault deals with the *nearest* eligible vault on its channel, so several on one
channel means the closest wins.

### Why there is no true mirror

The obvious ask — both barrels holding the same items, both usable — cannot be made
safe. Mirroring one stack into two barrels gives it two extraction points: two
players, or two hoppers, pulling in the same tick each walk away with a copy, and
the duplication has already happened before any function runs. Container clicks are
not interceptable from a data pack. So there is exactly one real copy of everything
in a channel, always, and the modes differ in how it gets to where you want it.

### `shared` — the contents follow you

The default, and the closest thing to one inventory. Stand within 4 blocks of any
vault on the channel and the whole contents move into *that* barrel: whichever
vault you walk up to is the one holding everything, and the far end is empty while
you are at this one.

The move is a single `data modify … Items set from block …`, and the source is only
cleared if that copy reported success — so the stacks are in one barrel before the
pair of commands and in one barrel after, never in both and never in neither.

Two people at two ends of one channel do not fight over it: a holder with someone
standing at it keeps what it has, rather than having it pulled away mid-click. And
if you put something into a vault while the far end still holds the rest, the two
are merged a stack per cycle through the free-slot path, because copying the whole
list over would collide slot numbers.

Hoppers and pipes should use `link` or the one-way modes instead — `shared` follows
players, not machines.

### `link` — two-way, and why each medium does it differently

`link` is the automation mode: put something in either end, take it out of the
other, no player needed. The naive version of that shuffles forever — A hands its
only stack to B, B now has more than A and hands it straight back — so each medium
gets the rule that suits it.

**Items follow the outside world.** Each vault remembers how many stacks it left
behind (`data.data.last_used`) and compares that with what is there now:

| Since last cycle | What happens |
| ---------------- | ------------ |
| More stacks — something was inserted | Push one stack to the partner |
| Fewer stacks — something was taken out | Pull one stack back from the partner |
| Unchanged | Nothing. A quiet pair costs nothing per tick |

A delivery updates the receiver's mark immediately, so an arriving stack is never
mistaken for an insert and bounced back.

!!! note "It counts stacks, not items"
    The mark is the number of stacks in the barrel, so taking *part* of a stack — 32
    out of 64 — does not read as an extraction and does not pull a refill. Take a whole
    stack, or use `shared` mode, where the contents come to you regardless. Pull is not a second copy of the move
logic: the asking vault makes itself the only eligible receiver for one command
and runs the partner's push.

**Fluid and EU find their level,** since neither is made of discrete stacks. Only
the fuller side pushes, and it pushes half the gap, so the two converge instead of
trading the same charge back and forth. A dead zone — 20 units of fluid, 4 EU —
stops the last few units oscillating.

One-way `send` / `receive` is still there for automation that wants a definite
direction: a `send` vault drains itself into the channel no matter what the far end
is doing.

### Item Vault

A real barrel, so hoppers, Item Pipes, Boxers and your own hands all work on it
with no special case. Every 4 ticks — hopper rate — a vault that owes a move sends
**one whole stack** into the first free slot of its partner.

The move is `/item replace … from block …` followed by clearing the source slot,
via `ra_lib:inventory/move_slot`. The destination slot is checked empty first,
because `item replace` overwrites whatever is in the way, and an overwrite would
destroy items. A full partner means nothing moves at all.

### Fluid Vault

An ordinary fluid-network node with a 1000 buffer, so pipes, pumps, tanks and
valves treat it like a tank. Every 10 ticks it moves fluid: up to `transfer_rate`
in one-way mode, or half the gap between the two networks in `link` mode.

Order matters: **take, then offer, then hand back the remainder.** A receiving
network that is full, or that holds a different medium, accepts less than was
offered, and whatever comes back is returned to the network it came from. The
fluid is in exactly one network at every point.

### Power Vault

Tagged as an electric node with a 400 EU buffer, so wires, generators and
consumers connect to it the way they connect to a switch. Every 5 ticks it moves
EU: up to `transfer_rate` one-way, or half the gap in `link` mode, and always
limited by the room actually available in the partner — the receiver reports what
it took, and only that much leaves the sender.

## Teleport Anchor

`minecraft:crying_obsidian` with an id and a table of fifteen target ids, one per
redstone signal strength.

| Property | Meaning |
| -------- | ------- |
| `anchor_id` | A **string** — `"A"`, `"base"`, `"mine_2"`. What other anchors aim at |
| `targets` | Fifteen ids: `targets[0]` is signal 1, `targets[14]` is signal 15. An empty string means unused |

Power the anchor and stand within **2 blocks**: the strength of the signal picks a
row of the table, and the nearest player is moved on top of the anchor whose
`anchor_id` matches. A redstone block reads as strength 16 and is treated as 15,
since the table stops there.

```
/function ra_ender:tools/anchor/set_id {id:"A"}
/function ra_ender:tools/anchor/set_table {table:["A","B","C"]}
/function ra_ender:tools/anchor/set_target {level:3,id:"B"}
/function ra_ender:tools/anchor/show
/function ra_ender:tools/anchor/help
```

Those act on the nearest anchor within 6 blocks. `set_table` takes the whole table
as typed text — signal 1 first — and pads the rest with empty rows, so
`{table:["A","B"]}` wires strengths 1 and 2 and nothing else.

The Data Handler edits both properties directly now: `anchor_id` gets a text row and
`targets` an `[Edit list]` row where you write the whole list — `["A","B","C"]` — in
the input book. That works because the Handler picks its editor from the value's
type rather than from a hand-written row per property name, which is why the table
used to be visible but unchangeable. See
[Creative Data Handler](developer-guide.md#creative-data-handler).

The Goggles show the live picture: the anchor's id, the current signal, and the id
that signal points at.

Guards worth knowing:

- An anchor pointed at its own id does nothing.
- An arriving player gets 30 ticks of grace, so landing on an anchor that is
  itself powered does not bounce them straight back. That also makes a two-way
  door work: wire both ends, walk in, walk out.
- Each anchor waits 20 ticks between teleports.
- A target id with no anchor, or a disabled one, is a no-op rather than an error.

## Scoreboards and tags

| Name | Meaning |
| ---- | ------- |
| `ra.ender.cd` | Per-vault work timer |
| `ra.ender.tp_cd` | Ticks before an anchor may fire again |
| `ra.ender.grace` | Ticks before a player may be teleported again |
| `ra.ender.recv_item` / `recv_fluid` / `recv_power` | Recomputed each tick: this vault can accept |
| `ra.ender.send_item` | Recomputed each tick: this vault can be pulled from |
| `ra.ender.self` | Held for one cycle so a vault never links to itself |
| `ra.ender.share` | Recomputed each tick: this vault is in `shared` mode |
| `ra.ender.occupied` | A player is within 4 blocks of this vault |
| `ra.ender.pull_target` | The vault a refill is being fetched for, for one command |
| `ra.ender.tp_dest` | The anchor a teleport is aimed at, for one command |

Channels are strings, and no selector can compare one entity's property with
another's, so the match happens in a macro: the sender writes its channel into
storage and the generated command carries it into the selector.

A two-way vault wears the send *and* the receive tag, so every partner search has
to exclude the vault doing the searching — that is what `ra.ender.self` is for, and
why each vault's cycle exits through `ra_ender:link/done` even on an early return.
