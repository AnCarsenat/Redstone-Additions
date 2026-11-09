# Settings

Two kinds of settings:

| | Your settings | Server settings |
|---|---|---|
| Who | everyone | operators |
| Open with | `/trigger ra.settings.open` | `/function ra:settings` |
| Affects | only you | the whole world |
| Examples | machine sounds, particles | generator output, which blocks may be placed |

Both commands are shown in chat when the pack loads.

## Your settings

```
/trigger ra.settings.open
```

No permissions needed. Click a value to change it.

| Setting | Default | What it does |
|---|---|---|
| Machine sounds | on | Sounds made by Redstone Additions blocks |
| Machine particles | on | Particles made by Redstone Additions blocks |
| Debug messages | off | Extra diagnostic messages in chat |
| Show filter previews | on | The item an Item Pipe filters for, floating above its Goggles label |
| Jetpack fuel gauge | on | The [fuel bar](jetpacks.md#fuel-gauge) on the action bar while you fly an Iron jetpack |

These only change what **you** see and hear — with one exception. Goggles labels are shared:
everyone near a block sees the same label. So *Show filter previews* decides whether **you** ask
for the floating filter item, and a pipe draws it while any Goggles wearer within range of it has
the setting on. With yours off you can still see a preview drawn for another wearer next to you.
Hover a setting's name in the menu to read this there.

## Server settings

```
/function ra:settings
```

Needs permission level 2 (operator). The first page is a row of buttons, one per module; each
opens a page with buttons beside every value: step it up or down, or **edit** to type an exact
number. Some pages have **subcategories**, listed at the bottom of the page (General has
**Goggles**); a subcategory's **[Back]** returns to its page.

Every setting is also a command, so you can tab-complete your way through them:

```
/function ra_settings:admin/wires/generator_eu_tick/up
/function ra_settings:admin/wires/generator_eu_tick/edit       (type an exact value)
/function ra_settings:admin/wires/electric_furnace/disable
```

### Typing a value

**edit** asks for the value in one of two ways:

- **A number** is typed with `/trigger ra.input.trigger set <n>` (the chat shows a ready-made
  link). The prompt names the setting and its range — *Enter a number for Goggles range
  (4–64)* — and a value outside it is refused with the same range. The prompt waits 30 seconds.
- **Text** is written on page 1 of an **Input Form** book; close the book to submit. You may
  hold the form in either hand. The prompt waits 5 minutes, and the wait starts again whenever
  you hold the form, so it never runs out while you are writing. Dropping the form, or putting it
  in a chest, bundle or lectern, cancels the prompt and the form disappears.

### What you can change

| Page | Settings |
| --- | --- |
| General | *Show the load message* (on): every line the pack prints in chat on load or `/reload` — the version, each module's "loaded!" line and the settings links. Warnings (some block types are disabled) are shown either way; chat prefix (`RA`); subcategory **Goggles** |
| General › Goggles | Goggles range (16 blocks); Goggles redraw (20 ticks); *Filter previews* (on): draw the item an Item Pipe filters for above its readout. Off here means off for everyone; on lets each player choose with *Show filter previews* |
| Logic Gates | default Clock interval (20), Delayer delay (20), Extender duration (20), Shortener pulse (2), Randomizer chance (50%), Counter target (10), Edge Detector pulse (2), Sequencer steps (4) |
| Interactive | default Magic Crate radius (8) and interval (20), Message Block range (16), Elevator range (16), Fan range (8) and strength (2) |
| Power & Fluids | default Generator EU/tick (60), Consumer EU draw (40), Industrial Light EU use (10), Liquid Drain rate (5000 mL per second), Usage Meter units per pulse (1000), Tesla Coil damage per zap (6) and EU per mob zapped (200) |
| Ender Links | default Power Vault EU/tick (80), Fluid Vault transfer (200); Anchor Tablet ender pearls per trip (1, 0 to 16) and recharge (10 seconds, 0 to 600) |
| Infinite Generators | default interval of each generator (100) |
| Jetpacks | Thruster Kit thrust (80%), speed cap (350), dead zone (25) |
| Enchant Crafting | enchant crafting on/off |
| Sensors | *Players may edit entity selectors and tags* (off): when on, players without server-settings access may change the selectors and tags of the Entity Detector, Tag Adder, Tag Remover and Message Block |
| every module | which of its blocks may be placed |

A few block settings are kept for players with server-settings access, whatever the pages say:
the Block Breaker, Block Placer, generator and Magic Crate cooldowns and the Liquid Drain rate.
A Message Block's **text** is no longer one of them (since v6.0.3): it is shown exactly as typed,
so any player may change it. Its selector follows *Players may edit entity selectors and tags*.

### Defaults for new blocks

Settings marked *(new blocks only)* are the value a block gets **when it is placed**. Blocks
already in the world keep their own value — press **[Apply to placed]** to push the new value onto
every existing block of that type (it tells you how many it changed).

!!! warning "[Apply to placed] overwrites individual tuning"
    A block you set by hand with the Wrench or Data Handler gets the new value too.

### Turning blocks off

Every block has an enable / disable switch. A disabled block cannot be placed — the item is given
back. Blocks already placed keep working, and the item can still be crafted.

```
/function ra_settings:disabled
```

lists every disabled block with an **[Enable]** button (also the **[Disabled blocks]** button on
the first page). While any block is disabled, the pack says so in chat on every load.

### Letting someone use the buttons

The buttons on the server pages work for players tagged `ra.admin`. Opening the pages as an
operator gives you the tag automatically.

```
/function ra_settings:admin/grant      give the nearest player access
/function ra_settings:admin/revoke     take it away
```

Admins (players with this tag) also go past the players' maximum of a range or radius, in the
Data Handler and on the placement-default rows here (Magic Crate radius up to 64, Message Block
range up to 1024, Elevator range up to 384, Fan range up to 64 and strength up to 10); see [Tools](tools.md).

!!! warning "Access stays until you revoke it"
    The tag survives reloads and is **not** removed when someone loses operator status. Use
    `/function ra_settings:admin/revoke` (or `/tag <player> remove ra.admin`).

## Purging entities

**[Purge entities]** on the first server-settings page (or `/function ra_settings:admin/purge/show`)
removes Redstone Additions' **entities** and keeps the pack installed: settings, disabled blocks,
player preferences and scoreboards stay. Placed blocks stay too, as ordinary vanilla blocks.

| Option | Removes | Keeps |
|---|---|---|
| **Everything** | every RA marker (so every machine, network and multiblock), block skins, pipe and wire displays, goggles readouts, RA-tagged dropped items, abandoned input books; releases the chunks powered Chunk Loaders kept loaded; clears transport networks, the Teleport Anchor destinations table and pending Data Handler edits | settings, disabled blocks, player preferences, clipboards |
| **Orphaned displays** | skins, pipe and wire displays and floating block items with no block under them | working blocks, goggles readouts |
| **Goggles readouts** | the floating goggles text and icons (redrawn for anyone still wearing goggles) | everything else |
| **Dropped RA items** | RA-tagged items on the ground (including a dropped Clock, UNI Gate or Message Block nobody picked up) and Data Handler or settings input books lying around | everything else |

Clicking an option **removes nothing**. It prints, in red, what will go and how many of each are
loaded right now, with **[CONFIRM]** and **[Cancel]**. **[CONFIRM]** works only for you, only for
that option, and only for 30 seconds; after that, review again. You get a summary of what was
removed, and every other player with server-settings access is told who purged what.

!!! warning "Only loaded chunks"
    Minecraft can only reach entities in loaded chunks, so a purge counts and removes what is
    loaded. After **Everything**, the Purge page shows **Unloaded chunks: cleaning**: every RA
    block from before the purge is removed (marker, skin and displays, and its Chunk Loader
    forceload) the moment its chunk loads. Blocks placed after the purge are never touched.
    **[Stop]** ends that (old blocks then come back as they load, with their networks gone);
    **[Resume]** starts it again. Purging **Everything** again restarts it.

There is no undo — copy the world first if you want a backup.

## Uninstalling

```
/function ra:uninstall
```

(also a button at the bottom of the server settings). It asks **twice**; the second time it lists
exactly what will be removed: every machine becomes an ordinary block, every network, multiblock
and display is removed, and every setting is erased. There is no undo — copy the world first if
you want a backup.

---

## Advanced

### Triggers

`/trigger` is how players without permissions click buttons. Triggers are only enabled while they
are usable, so your `/trigger` list stays short:

| Trigger | Used by | Enabled |
|---|---|---|
| `ra.settings.open` | Your settings (`set 2` = server settings, `set 3` = disabled blocks) | always |
| `ra.settings.act` | which row of a settings menu you clicked | while a menu is open, for a minute after your last click |
| `ra.settings.admin` | which server-settings button you clicked; bare, opens the index | for `ra.admin` holders |
| `ra.wrench` | Wrench menu | while holding the Wrench, plus 10 seconds |
| `ra.dh.action`, `ra.edit_type` | Data Handler menu | while holding the Data Handler, plus 10 seconds |
| `ra.input.trigger` | number prompts ([Typing a value](#typing-a-value)) | while a number prompt waits for you |
| `ra.jp.mode`, `.sound`, `.power`, `.kits` | [Jetpacks](jetpacks.md) | while wearing a jetpack |

The two uninstall confirmations use ordinary command links on purpose, so Minecraft's own
"run this command?" dialog stays in the way.

### Storage and reading settings

- Server values live in `storage ra:settings global`; per-player values are scoreboard objectives
  (`ra.u.snd`, `ra.u.par`, `ra.u.dbg`).
- From functions:

  ```mcfunction
  function ra_settings:get     {key:"welcome",default:1}
  function ra_settings:prop    {block:"electric_generator",prop:"generation_rate",default:60}
  function ra_settings:user    {obj:"ra.u.snd",default:1}
  function ra_settings:enabled {block:"electric_furnace"}
  ```

  The first three leave the answer in `#setting ra.set.tmp` and also return it; `user` must run
  as the player. `enabled` returns 1 when the block may be placed.
- Settings pages are Kotlin data in `ra/settings/pages/`; adding a setting is described in
  [Settings Internals](settings-internals.md).
