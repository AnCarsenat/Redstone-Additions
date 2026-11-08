# Settings Internals

How `ra_settings` is built. What the settings do in game, and the rules for choosing a scope,
are in [Settings](settings.md); they are unchanged.

In the beet pack, each page was a JSON file in `tools/settings/`, and `tools/settings_gen.py` (a
beet plugin) generated the functions that depend on which settings exist. In the rewrite the
pages are Kotlin data in `ra/settings/pages/`, and the generator is Kotlin in `ra/settings/gen/`.
Both run in the same `koreBuild`, so nothing can fall out of step.

## Pages

```kotlin
// ra/settings/pages/GatesPage.kt
val GatesPage = Page(
	"gates", "Logic Gates", Ns.GATES,
	listOf(
		PropRow("clock", "cooldown", "Clock interval", default = 20, min = 2, max = 1200, step = 2),
		PropRow("delayer", "delay", "Delayer delay", default = 20, min = 1, max = 1200, step = 5),
	) + blocks(
		"uni_gate" to "Universal Gate",
		"clock" to "Clock",
	),
)
```

`Page(id, title, namespace, rows, parent = null)`. A page with a `parent` is a **subcategory**:
it is left off the server settings index, its parent's page lists it under "subcategories", its
title bar reads `Parent › Title` and its Back returns to the parent. One level deep, and both
pages need server rows; the generator fails the build otherwise. `GogglesPage` (parent
`general`) is the one today. Every page is listed in `SettingsPages.all`
(`pages/SettingsPages.kt`), in the order the beet build read its JSON files: sorted by file name.
**The order is observable.** It numbers the admin pages and every clickable action code, and
those codes are what the chat buttons on a player's screen carry.

To add a page: add a file, then add it to `SettingsPages.all`. Appending is safe; inserting
shifts every later code.

## Rows

All in `pages/Row.kt`.

| Row | Fields | Stored | Reached by |
| --- | --- | --- | --- |
| `BoolRow` | `key, label, scope, default, obj?` | `0` / `1` | global: `admin/<page>/<label>/{on,off}`; user: menu |
| `IntRow` | `key, label, scope, default, min, max, step, obj?` | an int, clamped | global: `up`, `down`, `edit`, `reset`; user: menu |
| `StrRow` | `key, label, scope, default, obj?` | text, typed through `ra_lib:input` | global: `edit`, `reset`; user: menu |
| `ListRow` | `key, label, scope, default, values, obj?` | the **index** into `values` | cycles |
| `BlockRow` | `block, label` | `storage ra:settings disabled` | `admin/<page>/<block>/{enable,disable}` |
| `PropRow` | `block, prop, label, default, min, max, step` | `storage ra:settings` per block | `up`, `down`, `edit`, `reset`, `apply_to_existing` |
| `NoteRow` | `label` | — | text only |
| `SepRow` | `label` | — | a rule |

`blocks("id" to "Label", …)` makes a run of `BlockRow`s.

### Scope

| | `Scope.GLOBAL` | `Scope.USER` |
| --- | --- | --- |
| Lives in | `storage ra:settings global` | a per-player objective (`obj`) |
| Changed with | `/function ra_settings:admin/…` (permission level 2) | `/trigger ra.settings.open` (no permission) |
| In the menu | no | yes |
| Use for | anything that changes game balance | what one person sees or hears |

A user row **must** name `obj`, short (under 16 characters) and stable: a player's choice is
found by that name, so renaming it resets everybody. The generator fails the build if it is
missing. The only user rows today are on `YouPage`: `ra.u.snd`, `ra.u.par`, `ra.u.dbg`
(`Obj.USER_SOUND`, `USER_PARTICLES`, `USER_DEBUG`).

`ListRow` stores the index rather than the string, so renaming a choice moves the label without
orphaning what anyone had selected.

## What is generated

`SettingsGenerator(pack, SettingsPages.all).generate()` walks the pages once, in order, handing
out action codes as it meets them (`gen/ActionTable.kt`), then `gen/Registries.kt` writes the
tables:

| Generated | Contents |
| --- | --- |
| `ra_settings:pages` | the `/trigger` menu registry: **user rows only** |
| `ra_settings:defaults` | seeds every setting that has no value yet and creates user objectives; never overwrites a chosen value |
| `ra_settings:sync` | per-tick seeding for players with no score yet |
| `ra_settings:admin/<page>/show` | one overview page per page with global rows |
| `ra_settings:admin/<page>/<label>/<action>` | one real function per action (`gen/AdminRows.kt`) |
| `ra_settings:admin_actions` | action code → function, read by the dispatcher in one macro call |
| `ra_settings:admin_pages` | code → page, for the redraw after an action |
| `ra_settings:blockmap` | block → label + re-enable code, for `ra_settings:disabled` |
| `ra_settings:uninstall` | removes exactly what the generator created |
| `#ra_settings:register` | `pages`, `defaults`, `admin_actions`, `admin_pages`, `blockmap`; called from `ra_settings:init` on every load |

`<label>` is the row's label as a path segment: `Clock interval` → `clock_interval`,
`Goggles range (blocks)` → `goggles_range_blocks`. Because the generator knows every key, bound
and step at build time, the admin tree contains **no macros**: none of the quoting fragility of
the runtime applies to it.

Everything else in `ra/settings/` is the hand-written runtime: the menu (`Menu.kt`, `page/`,
`row/`), the click and edit flow (`act/`, `Edit.kt`, `apply/`, `ApplyStr.kt`), the operator
panel (`Admin.kt`), the disabled-blocks list (`Disabled.kt`), placement (`placement/`) and the
read API (`Read.kt`).

### Typed values

An `edit` action or a typed user row writes what is being edited into `storage ra:settings edit`
and calls `ra_settings:edit_start`. That slot is only a working copy: once the input request is
open, the payload moves to `edits.r<id>`, keyed by the player's `ra.settings.req`, and `edit` is
emptied. `ra_settings:apply_pending` copies the player's own slot back into `edit` for the one
call that applies it. Every path that stops the wait (applied, session gone or expired, another
tool's request replacing it, a new edit started) removes `edits.r<id>` through
`ra_settings:edit/drop` or `edit/forget`. `global` and `prop` edits are applied only to a player
who still holds `ra.admin` when the value arrives; the generated `edit` actions grant it first,
as `admin/show` does.

## Reading a setting

Through `ra.api.Settings`:

| Reference | Macro arguments | Returns |
| --- | --- | --- |
| `Settings.get` | `{key, default}` | the global value |
| `Settings.prop` | `{block, prop, default}` | a block property default |
| `Settings.user` | `{obj, default}` | the executing player's value |
| `Settings.enabled` | `{block}` | whether that block type may be placed |

```kotlin
execute {
	storeResult { score(literal("#welcome"), Obj.TEMP) }
	run { function(Settings.get, nbt { this["key"] = "welcome"; this["default"] = 1 }) }
}
```

**The default is required, and must be the value that keeps the feature working.** `get` once
returned 0 for a missing key, and 0 is a real value ("off", "disabled"), so a key missing for
any reason switched features off. A settings read must never be able to disable a module.

### Where block defaults apply

`PropRow` values are written onto the marker's `data.properties` **at placement**, by
`ra_settings:placement/seed`, which the router runs after the handlers. They are not read live:
`ra_lib:util/property` runs on every consumer, bridge and drain on every tick and is deliberately
four commands with no sub-calls. Applying at placement also stops a retune from silently
re-balancing existing builds. `apply_to_existing` is the explicit opt-in.

## Purge

The Purge page is generated with the index (`gen/AdminPurge.kt`); its buttons register their
action codes after every existing one, so nothing on screen changed meaning. What it runs is in
`ra/settings/purge/Purge.kt`.

| Function | Does |
| --- | --- |
| `admin/purge/show` | the page: one button per option, the clean-up status, Back |
| `admin/purge/<option>/review` | counts (`purge/<option>/count`), prints the warning, arms the player: `ra.purge` = the option's code, `ra.purge.t` = game time + 600 |
| `admin/purge/<option>/confirm` | re-checks `ra.admin`, the armed code and the deadline, disarms, counts again, runs `purge/<option>/run`, reports, tells the other `ra.admin` holders |
| `admin/purge/cancel`, `expired` | disarm |
| `admin/purge/cleanup/stop`, `resume` | set `#sweep ra.purge` |

Options are `all` (1), `orphans` (2), `billboards` (3), `items` (4). A count leaves one
`#p.<n> ra.set.tmp` per category and tags what it picked by hand (orphans, input books)
`ra.purge.hit`; the review removes that tag, the run kills it.

**Orphans** are display entities (`block_display`, `item_display`, `text_display`,
`interaction`, billboards excluded) with no RA marker within 0.6 blocks. Every block display is
summoned on its marker's position, so the two always share a chunk and an unloaded marker cannot
make a loaded display look orphaned. Markers whose block is gone are not an option: each block's
own tick already detects that and runs its break.

**Everything** also clears the world state that named the killed entities: `ra:transport nets`
(emptied) and its rebuild queues (`dirtyq`, `dirtyset`, `pending`, `full`, `dirty` = 0b),
`ra:ender anchor_ids`, `ra:dh pend` and the hand-over slots, and every player's `ra.dh.sel` /
`ra.dh.pending`. Scores on the killed entities go with them; allocators (`#net_seq`,
`#next ra.dh.mid`) are kept so no id is reused. Data Handler input sessions are cancelled through
`ra_lib:input/cancel`.

### Unloaded chunks: the purge epoch

`#epoch ra.purge` counts purges of everything (mirrored in `storage ra:purge cur.e` for macros);
`#sweep ra.purge` is 1 while the clean-up runs. Once a world has been purged, every persistent
marker is stamped `ra.pe.<epoch>` when it is summoned (`Settings.Purge.stampLine`, called beside
`stampNew` in placement and the infinite cauldrons, and in multiblock `setup_marker`). While
`#sweep` is 1, `ra_settings:purge/tick` (in `ra:tick`, before the migration pass) runs one
selector, `@e[type=marker,tag=ra,tag=!ra.pe.<epoch>]`: a marker never stamped and one stamped by
an older purge alike. Each one found releases its forceload, takes the displays within 0.6 with
it and dies (`purge/sweep_one`). Idle, the tick is one score test.

**A new persistent marker must call `stampLine`.** The build fails if a function summons an
`ra`-tagged marker it does not also kill and nothing it calls stamps it
(`Purge.unstampedMarkers`), because after a purge the clean-up would remove that marker the next
tick.

## Ordering

- `ra_settings:init` runs in `ra:load` **after** every module's load; it creates the purge
  objectives (`ra_settings:purge/init`).
- `ra_settings:purge/tick` runs right after the tool tag cleanup in `ra:tick`, before
  `ra_migrations:entities`: a leftover marker is removed before anything migrates or ticks it.
- `ra_settings:tick` (seeding, menu clicks) runs early in `ra:tick`, before module ticks filter
  sounds and particles on `ra.u.snd` / `ra.u.par`.
- `ra_settings:input_tick` runs **after** `ra_lib:input/tick`, so a form submitted this tick is
  collected this tick. Opening an input session before `input/tick` gets it scanned in the tick
  it was created, which the book backend does not expect.
