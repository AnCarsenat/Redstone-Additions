# Design note: item and signal networks (#47)

Status: explanation, to decide whether either is in v6.0.3. No code has changed.

The brainstorm proposed two new kinds of network on the transport engine:

- an **item network**: pipes that pool items the way tanks pool fluids, with drawers that hold
  them and import/export points that move them in and out of containers;
- a **signal network**: cables that carry the highest redstone level of their sources to every
  output on the cable.

This note says what each would be, what it would reuse, what it would cost, how it overlaps with
what exists, and recommends when (or whether) to build it.

## What the framework already gives

The engine is `ra_lib:transport` (`ra/lib/transport/`). It does three things:

1. **Grouping.** Adjacent node markers of the same class (`ra.tr.class`) are flood-filled into
   numbered networks when one is placed or broken (`rebuild/`). Classes are a table in
   `Transport.init`: `fluid` 1, `item` 2, `electric` 3. Adding a class is one entry; the flood
   compares the class as a score, so it costs nothing extra (`Flood.spread`).
2. **A pooled record per network**, `storage ra:transport nets.n<id>`: `amount`, `capacity`,
   `nodes`, and a per-medium breakdown `amounts` / `media`. Each node adds `ra.tr.cap` to the
   capacity. Contents are **integers per named medium**.
3. **Moving contents**: `net/offer`, `net/take`, `net/take_any`, `net/read` (`net/Flow.kt`,
   `net/Read.kt`), and the rebuild carrying contents across splits and merges: parked on the
   root (`rebuild/snapshot`), folded back (`rebuild/absorb`), trimmed to capacity
   (`rebuild/clamp`). On a split, **all** contents go with the part that kept the old root, and the
   other part starts empty.

Pipes, wires and tanks do no work per tick; only blocks that move something are ticked.

The `item` class already exists: **Item Pipes join it** with capacity 0
(`ra/modules/interactive/ItemPipe.kt`, `handlePlacement`), but nothing reads the item networks.
Item Pipes still move stacks hop by hop, every 4 ticks (`ItemPipeTransfer.kt`). So today item
pipes pay for being in the rebuild (they count towards the #42 ceiling) and get nothing from it.

## The item network

### What it would be

- **Item pipe** (a new conduit, or today's Item Pipe): connects, holds nothing.
- **Drawer**: adds capacity to the network, for example 8 or 32 stacks. The network's items are
  "in the drawers", but as one pool, like fluid is "in the tanks".
- **Import point**: on a container's face; takes stacks out of it into the pool (optionally
  filtered), every few ticks.
- **Export point**: on a container's face; puts stacks from the pool into it (optionally
  filtered).
- Probably a **terminal** to see and take what the network holds, because a pool you cannot look
  into is not much use.

That is a small storage system in the style of Applied Energistics or Refined Storage.

### What is reused, what is new

| Part | Reused | New |
| --- | --- | --- |
| Grouping, network numbers, out-of-view holding | All of `rebuild/` | Nothing |
| Capacity | `ra.tr.cap`, `capacity` | Measured in stacks, not mL |
| Contents | **Nothing.** `amounts` is integer per medium name; an item is an id, a count and any number of components (enchantments, names, contents of a shulker box, …) | A list of stacks in the record (`nets.n<id>.items`), with insert-merge, take-with-filter and count |
| Splits and merges | The root-carries-everything model | Concatenating two item lists and merging equal stacks; and a clamp that cannot just trim: items over capacity after a drawer is broken must be **dropped in the world**, or they are destroyed |
| Moving to and from containers | `ra_lib:inventory` (`move_slot`, `find_free_slot`) | Import/export blocks, filters, a terminal UI |

### Cost

- Comparing an item with each stack in the pool is the compare trick (copy, overwrite, test
  success): ~4 commands a stack. An insert into a pool of S distinct stacks costs ~4 S. A network
  with 500 distinct stacks makes every insert ~2,000 commands; ten import points every 4 ticks is
  ~5,000 commands a tick.
- The pool is one storage compound rewritten on every change. Big shulker-box-filled pools make
  that compound large, and it is saved in `command_storage_ra.dat` with every other network.
- A rebuild of an item network must carry and re-merge the list: proportional to the pool, on top
  of the per-node flood.

### Overlap

- **Item Pipe** already carries and sorts items between containers.
- **Ender Item Vault** already moves stacks between two places, anywhere.
- **Vanilla**: hoppers, and chests as storage. Drawers would compete with chests, which already
  hold items with no command cost at all.
- **Boxer / Unboxer** (`docs/storage.md`) already pack a container into one item.

### Scope

Large: 4-5 new blocks, a new contents model in the engine, a loss-free split and clamp, a
terminal UI, migration for the existing Item Pipes' class, docs, recipes, refunds, wrench rows.
Several weeks, and it adds a new way to lose or duplicate items to the engine that so far has had
none.

## The signal network

### What it would be

- **Signal cable**: connects, holds nothing. Unlike dust, no decay over distance, does not connect
  to vanilla redstone on the sides, and can run vertically.
- **Signal input**: reads the redstone level (0-15) arriving at it.
- **Signal output**: gives out the network's level: the **highest** level of all inputs on the
  network.

### What is reused, what is new

| Part | Reused | New |
| --- | --- | --- |
| Grouping | All of `rebuild/`, a new class `signal` 4 | Nothing |
| Contents | Nothing to carry: the level is recomputed from the inputs, so capacity 0, no snapshot, no clamp | A per-network level, kept as a score on the network's root or in `nets.n<id>.level` |
| Reading a level | `ra_lib:redstone` already reads analog levels 1-15 (`lib/redstone/Rules.kt`) | — |
| Giving out a level | The output shell (`ra/core/block/Redstone.kt`, `outputShell`): iron blocks become redstone blocks, which is **on/off at 15 only** | An exact level 1-15 needs another way out, for example the output block holding a calibrated item count so a comparator reads that level |
| After a rebuild | — | A hook: the networks a rebuild touched must recompute their level (a split may lower it, a merge raise it). The engine has no per-class callback today |

How "highest" is computed cheaply: when an input's level changes, recompute its network with one
command, because a scoreboard operation over many entities applies to each in turn:

```
scoreboard players set #lvl ra.tr.tmp 0
scoreboard players operation #lvl ra.tr.tmp > @e[type=marker,tag=ra.sig.input,scores={ra.tr.net=<id>}] ra.sig.level
```

then update that network's outputs only if `#lvl` changed.

### Cost

- **Inputs** are polled every tick, like a logic gate (a few commands each), because vanilla does
  not tell a data pack when a signal changes.
- **On a change**: two commands for the level, plus one update per output on that network.
- **Cables**: nothing per tick. They do join the rebuild, and long redstone cable runs are exactly
  the kind of big network #42 is about.

### Overlap

- **Wireless Redstone** (`docs/wireless-redstone.md`) already carries a signal anywhere by
  channel, but on/off only (any powered emitter turns the receivers on).
- **Vanilla dust** carries levels but decays by one per block; repeaters restore 15 and lose the
  level.
- **Logic gates**: the planned Signal Calculator (v6.0.3, section 3) works on levels; a signal
  cable would feed it nicely, but it does not need one.

The new thing a signal network offers is **an analog level carried over any distance without
decay**, which nothing in the pack or vanilla does.

### Scope

Medium: 3 blocks (cable, input, output), a new class, a level per network, a post-rebuild hook in
the engine, the exact-level output (the part that needs a small prototype first), docs, recipes,
wrench rows, refunds, uninstall. About the size of two or three ordinary blocks plus the engine
hook.

## Recommendation

**Signal network: later, in the release after v6.0.3,** once #42 has landed.

- It fits the engine well (no contents to carry, one score per network) and offers something
  nothing else does.
- But it is new engine work (a post-rebuild hook) on top of v6.0.3's already full list, and the
  exact-level output needs a prototype first. And it makes big networks more common, so it should
  come after the rebuild can handle them (#42).
- Before building it: prototype the exact-level output in game; if no clean way exists, ship
  on/off outputs first and say so.

**Item network (pooled): never in the pooled form.**

- Items are not numbers. A pool needs a new contents model, a loss-free split and clamp, and a
  per-insert cost that grows with what the network holds, all to replace chests, which cost
  nothing.
- It would be the first part of the engine that can lose or duplicate items.
- If players want "pipes that deliver to the right place", the smaller step is **routed Item
  Pipes**: an import point hands a stack to the nearest export point on the same item network
  whose filter accepts it, with the stack going straight from container to container and never
  stored in the network. That uses the item class that already exists, needs no contents model,
  and could be its own design note later.

**In the meantime (small, could be v6.0.3 or later):** since nothing reads item networks, Item
Pipes could stop joining the transport engine, which takes them out of every rebuild. This needs a
look at `ItemPipe.kt` and its break handler (it leaves its network on break) and an entry in the
6.0.3 migration to remove them from their networks. Worth doing only if the routed pipes above are
not planned; otherwise keep the class for them.
