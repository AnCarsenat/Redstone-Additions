# Design note: Ender vault partner search (#43)

Status: proposal for v6.0.3. No code has changed yet.

## The problem

Every time a vault moves something, it looks for its partner: the nearest vault on the same
channel that can receive. The search visits **every receiving vault loaded in the world**, so the
total cost grows with the square of the number of vaults.

### How the search works today

`ra_ender:link/pick` (`ra/modules/ender/link/Link.kt`, `Link.pick` and `Link.pickMatch`):

```
execute as @e[type=marker,tag=$(recv),tag=!$(exclude),sort=nearest] run function ra_ender:link/pick_match
```

`pick_match` compares the candidate's `data.properties.channel` with the wanted one without a
macro: it copies the channel to `storage ra:ender pick.cmp` and overwrites it with the wanted
channel; an overwrite that changes nothing is a match. That is about 6 commands for a candidate
that does not match, and 2 (the function call and the early `return`) for every candidate after
the first match. A channel never reaches a command line, which is why this design was chosen in
6.0.2 (player text in a selector or a macro could add commands).

So one pick costs about **2 R + 4 k** commands, where R is the number of loaded receivers of that
kind and k is the partner's rank by distance. With no partner on the channel, it is about 6 R.

### How often it runs

| Vault | Cycle | Picks per cycle |
| --- | --- | --- |
| Item Vault (`ItemVault.kt`, `process`) | 4 ticks | 1 when a stack was put in or taken out (push, or pull through `Link.requestItems`); 0 when idle in `link` mode; 1 every cycle in `send` mode while it holds items |
| Fluid Vault (`FluidVault.kt`, `process`) | 10 ticks | 2 in `link` mode while its network holds anything (`Link.probeFluid`, then `Link.sendFluid`); 1 in `send` mode |
| Power Vault (`PowerVault.kt`, `process`) | 5 ticks | 2 in `link` mode while its grid holds EU (`Link.probePower`, `Link.sendPower`); 1 in `send` mode |

A vault in `link` mode wears the receive tag, so R is roughly every vault of that kind.

### Numbers

50 pairs of Power Vaults in `link` mode, each pair on its own channel (100 vaults, R = 100):
100 vaults × 2 picks / 5 ticks = 40 picks a tick, × ~200 commands = **~8,000 commands a tick**,
for vaults that are mostly levelling grids that are already level. At 200 vaults it is ~32,000,
half the tick's command budget. Fluid Vaults are half that (10-tick cycle); idle Item Vaults are
free, busy ones are like Power Vaults.

The selector itself also sorts R entities by distance on every pick (Java side, not counted as
commands, but not free either).

The Teleport Anchor uses the same compare trick (`TeleportAnchor.kt`), but only when someone
teleports, so it is out of scope here.

## Options

### A. Index receivers by channel with a score

Give every distinct channel name a number, once, and keep it on the vault as a score
(`ra.ender.chan`). The search becomes one selector the game filters natively:

```
$execute as @e[type=marker,tag=$(recv),tag=!$(exclude),scores={ra.ender.chan=$(cid)},sort=nearest,limit=1] at @s run function $(run)
```

`$(cid)` is an integer the pack produced, never player text, so the macro is safe. The name to
number table lives in `storage ra:ender chan_ids` (`[{name:"default",id:1}, …]`) and is searched
with the existing compare trick, but only when a vault's channel **changes**, over the number of
distinct channels (usually a handful), not the number of vaults.

| | |
| --- | --- |
| Cost per tick | ~4 commands per pick whatever R is, plus ~3 per cycle to check the vault's own channel has not changed. 50 pairs of Power Vaults: ~40 picks × 4 + 20 cycles × 3 ≈ **220 commands a tick** instead of ~8,000 |
| Complexity | Medium: a channel table, an id allocator, a re-index function, and a check in each vault's cycle |
| Save compatibility | No migration: a vault with no `ra.ender.chan` score (every vault in an old world) indexes itself on its first cycle. The new objective and storage key join the uninstall footprint |
| Players see | Nothing changes: still the nearest vault on the channel, anywhere in the world |

How a channel change is noticed: each vault keeps the name it indexed in `data.data.chan_name`.
At the top of its cycle it compares `data.properties.channel` with it (the same compare trick,
one candidate). If they differ, or there is no score yet, it re-indexes. That catches every way of
changing a channel (Data Handler, `/data`, an old world) without a hook in the Data Handler.

A partner whose channel was just edited is found under its old number until its own next cycle
(4-10 ticks). That is invisible in practice; a hook in the Data Handler's property write could
re-index at once if it ever matters.

### B. Index receivers by channel with a tag

Tag each vault `ra.ender.ch.<channel>` and select by that tag.

| | |
| --- | --- |
| Cost per tick | Same as A |
| Complexity | Low, until the channel text: a tag allows only `A-Z a-z 0-9 _ . + -`, but a channel may also hold spaces, `:`, `/` and `#` (format `NAME`). Channels would need escaping, or a narrower format, which breaks existing channels |
| Save compatibility | Old channels with spaces would need renaming |
| Players see | Possibly a narrower set of allowed channel names |

Worse than A: it puts player text back into a selector (exactly what 6.0.2 removed), and needs a
tag per channel to be added and removed correctly whenever the channel changes. A is the same
idea with a number the pack controls.

### C. Cache the partner

Store the partner's UUID on the vault after a full search, and on later cycles check it directly
(`execute as <uuid>`, a macro over a UUID the game produced). Search again only when the check
fails.

| | |
| --- | --- |
| Cost per tick | Best case ~5 commands per pick (check the cached vault still exists, is still a receiver, still on the channel). Worst case unchanged: a vault with no partner searches every cycle |
| Complexity | Medium-high: invalidation. A cached partner stays valid while it exists, so a **nearer** vault placed later on the same channel is never chosen, unless every placement, break and channel edit bumps a global "vaults changed" counter that drops every cache. Item Vaults need two caches (push and pull partners use different tags) |
| Save compatibility | No migration; an empty cache just searches |
| Players see | Either "nearest" quietly becomes "first found", or every vault re-searches whenever any vault anywhere changes |

Good when partners are found, bad when they are not, and it changes the behaviour the docs
promise. The vault with no partner (a typo in the channel) stays expensive.

### D. Limit the search

Cap it: `limit=8` nearest receivers, or `distance=..256`.

| | |
| --- | --- |
| Cost per tick | Bounded per pick: ~16-50 commands |
| Complexity | Trivial |
| Save compatibility | None |
| Players see | **A broken promise**: "move things between two places that are nowhere near each other". With a distance cap, far vaults stop linking; with a count cap, a vault whose partner is the ninth nearest receiver silently stops |

Not acceptable as the fix.

### E. Do less searching

Probe less often, or skip a `link` cycle when nothing changed on either side.

| | |
| --- | --- |
| Cost per tick | A constant factor less (for example: Fluid and Power vaults probe only when their own amount changed since last cycle) |
| Complexity | Low-medium: one more remembered value per vault |
| Save compatibility | None |
| Players see | Levelling reacts a cycle later in some cases |

Still quadratic, just with a smaller constant. Could be added on top of A, but A alone already
makes a pick cheap enough that it is not needed.

### F. A channel registry of members

Keep, per channel, the list of vault UUIDs on it in storage, and pick the nearest from that list.

| | |
| --- | --- |
| Cost per tick | Proportional to vaults on the channel (usually 2) |
| Complexity | High: the list must be kept right on placement, break, channel edit, chunk unload and admin purge; finding the nearest of several still needs a selector |
| Save compatibility | Needs building on load for old worlds |
| Players see | Nothing |

More moving parts than A for the same result.

## Recommendation

**Option A, a channel number on each vault (`ra.ender.chan`), with self-indexing.**

It makes a partner search a constant few commands, keeps "the nearest vault on the channel,
anywhere" exactly as documented, keeps player text out of every command, and needs no
migration.

### Implementation sketch

In `ra/modules/ender/link/` (a new `ChannelIndex.kt`, ~100 lines):

- `load`: `scoreboard objectives add ra.ender.chan dummy`; `#ender.chan_seq` added with `add 0`
  (never reset, like `#net_seq`); `chan_ids` created empty if absent.
- `link/index` (as a vault): copy `data.properties.channel` to storage, run `ra_lib:safe/guard`
  (as today), walk `chan_ids` with the compare trick; on a match take its `id`, else allocate
  `#ender.chan_seq + 1` and append `{name, id}`. Store the id in `ra.ender.chan` and the name in
  `data.data.chan_name`. A channel failing the guard gets score 0, which nothing matches.
- `link/check_index` (as a vault, top of each `process` in `ItemVault`, `FluidVault`,
  `PowerVault`, **before** any early return so receive-only vaults stay indexed): run `index`
  when there is no score or the channel differs from `chan_name`.
- `Link.pick`: drop `pick_match` and the `ra.ender.pick` tag; read the asking vault's
  `ra.ender.chan` into the macro arguments as `cid`, and select with
  `scores={ra.ender.chan=$(cid)},sort=nearest,limit=1`. `partner()` passes `cid` instead of
  copying the channel to `pick.want`. A `cid` of 0 returns at once.
- Uninstall footprint (`PackFootprint.kt`) and admin purge: the objective (which
  takes `#ender.chan_seq` with it) and `ra:ender chan_ids`.
- The table only grows (one entry per distinct name ever used). If that ever matters, an admin
  command can rebuild it; not needed now.

Docs to update when it lands: `docs/ender-links.md`, Advanced: replace the paragraph on how
channels are compared, add `ra.ender.chan` to the scoreboard table.

Tests in game: two vaults on `default` link; renaming one end with the Data Handler moves the link
within a cycle; a nearer vault on another channel does not hide the partner (the 6.0.2 bug); a
vault on a channel with spaces and `#`; a world from 6.0.2 links straight after loading.
