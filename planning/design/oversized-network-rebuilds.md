# Design note: oversized network rebuilds (#42)

Status: proposal for v6.0.3. No code has changed yet.

## The problem

A transport network (pipes, wires, Item Pipes) is grouped by a flood fill whenever a node is
placed or broken, and once for every loaded network after a load. The flood runs inside
`ra:tick`, and a network is always rebuilt **whole or not at all**. A single network that is too
big for one tick can therefore never be rebuilt.

### Where the time goes

- `ra:tick` is one scheduled function (`ra/hub/Tick.kt`, `registerTick`). The game gives one
  function call one budget of commands: the `max_command_sequence_length` game rule, 65,536 by
  default. (Before 1.21.11 it was called `maxCommandChainLength`, which is the name our code
  comments still use.) Everything `ra:tick` calls shares that one budget: settings, migrations,
  the rebuild, then every module tick, the Goggles and the menu triggers.
- `ra_lib:transport/tick` (`ra/lib/transport/Transport.kt`, `RaLib.Transport.tick`) runs
  `rebuild/run` at most once every 5 ticks (`#rebuild_cd`).
- `rebuild/run` (`ra/lib/transport/rebuild/Rebuild.kt`, `Rebuild.run`) costs about **35-40
  commands per node** in scope (the figure in `Scope.FULL_BATCH_NODES`' comment). Most of it is
  per node and per neighbour:
  - `rebuild/spread` (`Flood.kt`): six `execute positioned … as @e[…,distance=..0.75]` lines,
    two score copies and the `#meet` test, for every node;
  - `rebuild/claim`: three more per node claimed;
  - `rebuild/accumulate_node` + `rebuild/accumulate` (`Absorb.kt`): two stores, a macro call and
    two `/compute` writes per node;
  - the tag sweeps at the start and end of `run`, and the wave tags in `rebuild/expand`.
- 65,536 / 40 ≈ 1,640 nodes with nothing else in the tick. With the rest of `ra:tick` on a busy
  server, the practical ceiling is **about 1,500 nodes** in one network.

### What happens at the ceiling

1. The rebuild has already run `rebuild/snapshot` (each network's record is deleted, its contents
   parked on its root's `ra.tr.carry`) and set every scoped node to network 0. Then the budget runs
   out mid-flood.
2. Everything after `ra_lib:transport/tick` in `ra:tick` is skipped for that tick: every module
   tick, the Goggles, the menu triggers. (The reschedule survives because it is the first line.)
3. `running:1b` is still set, so the next `transport/tick` sets `dirty` again
   (`Transport.kt`, the two `{running:1b}` lines). After the 5-tick cooldown the same rebuild runs
   again, with the same cost, and is cut at the same place.
4. Until it succeeds, which it never does, every node of that network is on network 0: machines on
   it see `no_network`, pumps and generators stop, and the contents sit on a root as `ra.tr.carry`.

So one big network costs the **whole pack one tick in five**, forever, and the network itself is
dead. It happens after every load (the `full` pass, `Scope.fullBatch`, always takes at least one
network "however big") and after any break inside it (the network is named in `dirtyq`).

Placing a node next to a big network is fine: adoption (`rebuild/adopt`) does not recount it.
Only a break, a meeting of two kept networks (`rebuild/meet`), or a load puts it in scope.

## Options

Costs are commands per tick on a server with one network of N nodes.

### A. Give up after N failed attempts and warn admins

Count consecutive cut rebuilds (a score bumped when `running:1b` is found). After, say, 3, stop
retrying the scoped networks (drop them from `dirtyq`/`fullq`) and `tellraw @a[tag=ra.admin]` a
warning naming the network and its node count.

| | |
| --- | --- |
| Cost per tick | Nothing while idle; the 3 failed attempts still cost 3 skipped ticks |
| Complexity | Small: a counter, a message, a list clean-up |
| Save compatibility | None needed |
| Players see | The pack no longer stutters, **but the network stays dead**: its nodes are on network 0 and its contents are parked on a root. The admin is told, and the only fix is to make the network smaller |

It stops the damage to the rest of the pack, but leaves the actual network broken. Worth having
as a safety net, not as the fix.

### B. Run the rebuild as its own tick entry

Call `ra_lib:transport/tick` from `#minecraft:tick` (or `schedule` it separately) instead of from
`ra:tick`. Each function in `#minecraft:tick`, and each scheduled function, gets its own
65,536-command budget. `#minecraft:tick` runs before scheduled functions, so the rebuild would
still run before the module ticks, as today.

| | |
| --- | --- |
| Cost per tick | Unchanged |
| Complexity | Very small: move one call, add one function tag entry |
| Save compatibility | None |
| Players see | A cut rebuild no longer stops machines, Goggles or menus. The network itself is still never rebuilt; the ceiling rises only from ~1,500 to ~1,640 nodes |

Cheap and worth doing regardless, because it makes any future overrun harmless to the rest of the
pack. On its own it does not fix #42.

### C. Raise the game rule

Admins can run `/gamerule max_command_sequence_length 262144`; the pack could even do it on load.

| | |
| --- | --- |
| Cost per tick | One rebuild of N nodes runs ~40 N commands in a single tick: 1,500 nodes is a 60,000-command spike, tens of milliseconds |
| Complexity | None |
| Save compatibility | None |
| Players see | Works, with a visible lag spike on every rebuild. Setting a server-wide rule from a data pack is not ours to do; it also lifts the safety net for every other pack. `max_command_forks` (also 65,536) would be the next limit for `execute as @e[…]` over many nodes |

Fine as advice in the docs for admins with giant bases, not as the fix.

### D. Make the rebuild cheaper per node

Two concrete savings:

- **One selector for the six neighbours.** The six face neighbours are exactly the markers at
  distance 1; diagonals are at 1.41 or more. `rebuild/spread` can claim them with one
  `execute as @e[type=marker,tag=ra.tr.node,distance=0.5..1.1,scores={ra.tr.net=0}] if score @s ra.tr.class = #cur_class ra.tr.tmp run function …/claim`
  instead of six `positioned` lines. `rebuild/adopt` and `rebuild/meet` can do the same.
- **Sum capacity per network, not per node.** `scoreboard players operation #cap ra.tr.tmp += @e[type=marker,tag=ra.tr.claimed,scores={ra.tr.net=<id>}] ra.tr.cap`
  adds every node's capacity in **one command**. Doing that once per network (and the same for
  `ra.tr.carry`) replaces `accumulate_node`'s per-node macro call and two `/compute` writes.

| | |
| --- | --- |
| Cost per tick | Every rebuild gets cheaper; a guess is 12-15 commands a node instead of 35-40 |
| Complexity | Medium: touches `Flood.kt` and `Absorb.kt`, the most delicate code in the engine (the invariant in `Rebuild.kt`) |
| Save compatibility | None |
| Players see | Nothing, except that the ceiling moves to roughly 4,000-5,000 nodes |

Good for everybody, but it only moves the ceiling. A big enough base still hits it.

### E. Split one network's rebuild across ticks

Make the flood resumable. The frontier is already held in entity tags (`ra.tr.frontier`), which
persist between ticks, so the breadth-first flood in `rebuild/expand` can stop after a budget of
nodes and pick up from the same frontier next tick.

The catch is what the world looks like between ticks. Today the scoped networks are taken apart
at the start (`snapshot`, nodes set to network 0) and only exist again at the end. Spread over
ticks, every machine on them would see `no_network` for the length of the rebuild. So the flood
has to work **in the shadow**: write the new assignment to a new score (`ra.tr.next`) and leave
`ra.tr.net` and the records alone until the flood is finished, then switch over in one tick.

| | |
| --- | --- |
| Cost per tick | Bounded: at most the budget (say 512 nodes ≈ 20,000 commands) per tick while a rebuild is running; the commit tick costs a few commands per node. Nothing while idle |
| Complexity | High: the rebuild becomes a small state machine (scope → flood over several ticks → commit), and it must cope with nodes placed, broken or unloaded while it runs |
| Save compatibility | Only transient state (one score, tags, a `job` compound in `ra:transport`). A load clears it and the existing `full` pass redoes the work. No migration |
| Players see | Every network rebuilds, whatever its size. A very large one takes a few ticks to show a split or a merge; until then it keeps working as it was |

The only option that removes the ceiling instead of moving it.

### F. Cap the intake of new nodes

Refuse a node (or keep it out of the network, on network 0) when its network already has, say,
1,024 nodes. `ra_lib:transport/net/join` would check the neighbour's record `nodes`.

| | |
| --- | --- |
| Cost per tick | None |
| Complexity | Small for placement; but a network can also grow past the cap by **merging** two networks under it, which needs a second check in `rebuild/meet` |
| Save compatibility | Existing worlds may already have networks over the cap: they still need one of the other options to load at all |
| Players see | An arbitrary limit: "your pipe does not connect", with a message. Grids in big bases are exactly the case the pack is meant for |

Hides the problem behind a rule players will hit and not understand.

### G. Skip unnecessary rebuilds of a big network

After a load, a network whose loaded node count equals its record's `nodes` and which is wholly
in view has almost certainly not changed. `rebuild/full_one` could leave it out of scope.

| | |
| --- | --- |
| Cost per tick | Saves the load-time rebuild of every unchanged network |
| Complexity | Small: one count (already made in `full_one`) and one comparison |
| Save compatibility | None |
| Players see | Faster settling after a load. Does not help a break inside a big network. A pipe removed and another added while the pack was off would go unnoticed until the next change |

A useful optimisation, but it does not fix the case a player causes by breaking a pipe.

### Other approaches considered

- **`/schedule` per wave.** Running each wave of `rebuild/expand` as its own scheduled call gives
  each wave its own 65,536 budget, in the same tick. It avoids the cut but not the spike (option
  C's lag), and still tears the network down for the duration. Option E with a budget per tick is
  the same idea without the spike.
- **Local repair around a break.** Only reflood the neighbourhood of the broken node to see whether
  the network actually split. Proving that a network is still connected needs, in the worst case,
  the whole flood anyway; it only wins when the network is a loop. Not worth the complexity.

## Recommendation

**Option E, split one network's rebuild across ticks, with B and A as cheap guards.**

- E is the only option that makes any network rebuildable, keeps the per-tick cost bounded, and
  needs no migration.
- B ships first (one call moved) so that, whatever else happens, a rebuild can never again stop
  the module ticks.
- A becomes a watchdog for E: if a job makes no progress for 200 ticks, abort it, put its
  networks back on `pending`, and tell admins once.
- D and G are worth doing later as plain optimisations; they are not needed for correctness.

### Implementation sketch

New state, all in `ra/lib/transport/rebuild/`:

- objective `ra.tr.next` (per node, the network a node will have after the job);
- `storage ra:transport job`: `{phase:"flood"|"commit", started:<gametime>}`;
- constant `TICK_BUDGET_NODES = 512` (the same figure as `FULL_BATCH_NODES`).

Flow:

1. **Start** (`rebuild/run`, when no job is running): choose the scope exactly as today
   (`Scope.scope`, including the `edge` hold). Do **not** snapshot. Set `ra.tr.next` 0 on scoped
   and unassigned nodes; set `phase:"flood"`.
2. **Flood** (each tick while `phase:"flood"`, from `transport/tick`): run adoption and
   `assign_next`/`expand` against `ra.tr.next` instead of `ra.tr.net`. `claim` adds 1 to
   `#job_claimed`; `expand` returns once it passes `TICK_BUDGET_NODES`, leaving the frontier
   tags for the next tick. When no frontier and no unassigned scoped node remain, set
   `phase:"commit"`.
3. **Commit** (one tick): first re-run the `edge` test on scoped nodes; if any chunk unloaded
   since the start, abort (clear tags, put the named ids back on `pending`). Otherwise do what
   `run` does today after the flood: `snapshot` the old records, copy `ra.tr.next` into
   `ra.tr.net` for claimed nodes, accumulate, absorb, clamp, clear tags.
4. **During a job**, `mark_dirty` only queues: a placed node stays on network 0 and is adopted by
   the next job; a broken node names its network in `dirtyq` for the next job. Machines keep using
   the old records, which stay valid until the commit.
5. `transport/init` drops any `job` left by a save mid-rebuild; the `full` pass rebuilds those
   networks anyway.
6. Add `ra.tr.next` to the uninstall footprint (`ra/hub/uninstall/PackFootprint.kt`) and to the
   admin purge (`ra/settings/purge/Purge.kt`).

Docs to update when it lands: `docs/transport-networks.md` (Advanced, "Engine" and "After a
load"), `docs/developer-guide.md` (Transport: cut rebuilds), and the comment on
`FULL_BATCH_NODES`.

Tests in game: a 3,000-pipe line with a tank at each end, broken in the middle (both halves get
their own network number, the contents go with the half that kept the root as today, and no
machine elsewhere stutters); the same world reloaded; a pipe broken in another network while
the big one is still being rebuilt.
