# Ender Links

Move things between two places that are nowhere near each other. Three **vaults** link two
endpoints by channel — items, fluids or power — and the **Teleport Anchor** moves players.

## Getting them

| Block | Looks like | Carries | Recipe |
| --- | --- | --- | --- |
| Ender Item Vault | barrel | item stacks | ![Ender Item Vault recipe](images/recipes/ra_ender/ender_item_vault.png){ width="220" } |
| Ender Fluid Vault | purpur block | liquids and gases | ![Ender Fluid Vault recipe](images/recipes/ra_ender/ender_fluid_vault.png){ width="220" } |
| Ender Power Vault | purpur pillar | EU (electricity) | ![Ender Power Vault recipe](images/recipes/ra_ender/ender_power_vault.png){ width="220" } |
| Teleport Anchor | crying obsidian | players | ![Teleport Anchor recipe](images/recipes/ra_ender/teleport_anchor.png){ width="220" } |

Operators: `/function ra_ender:items/give_all`.

## Vaults

### Linking two vaults

1. Place two vaults of the same kind, anywhere.
2. Give both the same **channel** with the [Data Handler](tools.md). New vaults start on
   `default`, so two fresh vaults are already linked.
3. Use them like the block they look like: hoppers, Item Pipes and your hands work on the Item
   Vault; [pipes, pumps, tanks and wires](transport-networks.md) connect to the Fluid and Power
   Vaults.

A vault always pairs with the **nearest** vault on its channel, so use one channel per pair.

### Modes

Shift + right-click a vault with the [Wrench](tools.md) to cycle its mode.

| Mode | Behaviour |
| --- | --- |
| `link` (default) | Two-way: put something in either end and it can be taken out of the other |
| `send` | One-way: this vault empties itself into the channel |
| `receive` | One-way: this vault only accepts from the channel |

There is never a copy: everything exists in exactly one vault at a time and moves between them.

### What each vault does

| Vault | How often | Moves |
| --- | --- | --- |
| Item Vault | every 4 ticks (hopper speed) | **one whole stack** into the first free slot of the partner. In `link` mode: put a stack in one end and it goes to the other; take a stack out and one is pulled back |
| Fluid Vault | every 10 ticks | up to `transfer_rate` (default 200) one-way; in `link` mode, half the difference between the two ends, so they even out. Holds 1000 |
| Power Vault | every 5 ticks | up to `transfer_rate` (default 80) EU one-way; in `link` mode, half the difference. Stores nothing itself: it is a link, not a battery |

!!! note "Item Vault counts stacks"
    Taking *part* of a stack out (32 of 64) does not count as taking it, so it does not pull a
    refill. Take whole stacks.

A full partner simply receives nothing; nothing is ever destroyed.

## Teleport Anchor

A pad that sends you to another anchor. Each anchor has a **name** (`anchor_id`) and a
**destination** for each redstone signal strength, 1 to 15 (`targets`).

### Setting one up

1. Place two anchors. Shift + right-click each with the [Data Handler](tools.md) and use
   **[Modify]** on `anchor_id` to name them, e.g. `Home` and `Mine`. Names may contain letters,
   digits, spaces and `_ . + - : / #`.
2. On `Home`, click **[List]** on `targets` in the Data Handler to show the fifteen strengths:

    ```
    targets: list (15/15) [Edit all] [List]
      Strength 1: (empty)  [Choose] [Edit]
      Strength 2: (empty)  [Choose] [Edit]
      …
    ```

    Click **[Choose]** on *Strength 1*: a list of every anchor named so far appears; click
    `Mine`. **[Edit]** types a name instead (for an anchor you have not placed yet), **[Clear]**
    empties a strength.
3. Stand on or next to `Home` (within 2 blocks) and power it with strength 1 (a lever on dust one
   block away, for example). You arrive on top of `Mine`.

Different signal strengths can send you to different anchors: strength 3 picks target 3, and so
on up to 15 (a redstone block counts as 15). If several anchors share the target id, you go to
the nearest one — never to the anchor you are standing on.

### Commands

These act on the nearest anchor within 6 blocks:

| Command | Does |
| --- | --- |
| `/function ra_ender:tools/anchor/set_id {id:"A"}` | Set this anchor's id |
| `/function ra_ender:tools/anchor/set_table {table:["A","B","C"]}` | Set targets for strengths 1, 2, 3…; the rest are cleared |
| `/function ra_ender:tools/anchor/set_target {level:3,id:"B"}` | Set the target for one strength |
| `/function ra_ender:tools/anchor/show` | Show the name and each strength that leads somewhere |
| `/function ra_ender:tools/anchor/list` | Show every anchor name known in the world |
| `/function ra_ender:tools/anchor/help` | Show this help in game |

The [Data Handler](tools.md) edits the same two values, without operator rights: `anchor_id`
as text, and `targets` one strength at a time (or the whole list at once with **[Edit whole
list]**, written like `["A","B","C"]`).

Wear the [Goggles](tools.md) to see an anchor's id, the current signal and where it points.

### Good to know

- After arriving you have 1.5 seconds (30 ticks) before you can be teleported again, so landing
  on a powered anchor does not bounce you straight back. A two-way door works: wire both ends,
  walk in, walk out.
- Each anchor waits 1 second (20 ticks) between teleports.
- A target id with no anchor does nothing.

---

## Advanced

### Properties

| Block | Property | Meaning |
| --- | --- | --- |
| Vaults | `channel` | string; vaults with the same channel are linked. Default `"default"` |
| Vaults | `mode` | `link`, `send` or `receive` |
| Fluid, Power Vault | `transfer_rate` | units per move in one-way mode (200 fluid, 80 EU) |
| Teleport Anchor | `anchor_id` | string, what other anchors aim at. Default `"A"` |
| Teleport Anchor | `targets` | fifteen ids: `targets[0]` is signal 1, `targets[14]` is signal 15; `""` is unused |

### Known anchor names

Every anchor files its name in `storage ra:ender anchor_ids` (in the order they were first
seen, at most 200) when it is placed or renamed, and remembers what it filed in
`data.data.registered_id`. A name leaves the list when the last loaded anchor carrying it is
broken or renamed. The list outlives the anchor's chunk, so an anchor kept loaded elsewhere (by a
Chunk Loader) can still be picked. The Data Handler's **[Choose]** reads this list (the schema's
`choices`).

The destination is found by comparing names, not through a macro: every other anchor's
`anchor_id` is copied to storage and overwritten with the wanted name, and an overwrite that
changes nothing is a match; the nearest match wins. Before 6.0.1 the name was pasted into an
`nbt=` selector, and a target containing `"}` could add commands to it.

An anchor whose `targets` is not a list of fifteen (an old Data Handler edit could store
anything) is repaired once, the first time it is loaded, by the 6.0.1 migration
(`ra_migrations:6.0.0-to-6.0.1/teleport_anchor`): `ra_ender:blocks/teleport_anchor/repair_targets`,
as the anchor marker, pads a short list with `""`, trims a long one and replaces anything else
with fifteen empty entries.

### How `link` decides

- **Items:** each vault remembers how many stacks it held last cycle (`data.data.last_used`).
  More stacks than before → push one stack to the partner; fewer → pull one back; unchanged →
  nothing. A delivery updates the receiver's count straight away, so an arriving stack is not
  bounced back. Stacks move with `ra_lib:inventory/move_slot` into a slot checked empty first.
- **Fluid and EU:** only the fuller side pushes, half the gap, with a dead zone (20 fluid, 4 EU)
  so the last few units do not oscillate. Fluid is taken, offered, and any remainder handed back,
  so it is always in exactly one network.

### Scoreboards and tags

| Name | Meaning |
| ---- | ------- |
| `ra.ender.cd` | Per-vault work timer |
| `ra.ender.tp_cd` | Ticks before an anchor may fire again |
| `ra.ender.grace` | Ticks before a player may be teleported again |
| `ra.ender.recv_item` / `recv_fluid` / `recv_power` | Recomputed each tick: this vault can accept |
| `ra.ender.send_item` | Recomputed each tick: this vault can be pulled from |
| `ra.ender.self` | Held for one cycle so a vault or anchor never links to itself |
| `ra.ender.pull_target` | The vault a refill is being fetched for, for one command |
| `ra.ender.tp_dest` | The anchor a teleport is aimed at, for one command |
| `ra.ender.tp_cand` | An anchor whose name matches, while the nearest is picked |
| `ra.ender.pick` | The vault picked as partner, for one command |

Vault channels are compared without a macro, the same way as anchor names (above):
`ra_ender:link/pick` visits the receiving vaults nearest first, copies each one's `channel` to
storage and overwrites it with the sender's, and the first copy that does not change is the
partner (it wears `ra.ender.pick` for one command). Vaults after the first match cost one command
each. Before 6.0.2 the nearest receiving vault was taken first and its channel checked
afterwards, so a closer vault on another channel hid the real partner. A channel may only hold
letters, digits, spaces and `_ . + - : / #` (format `NAME`), still checked by `ra_lib:safe/guard`
before each use; a vault whose channel fails the check links nothing.
