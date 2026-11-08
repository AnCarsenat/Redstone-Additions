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

These only change what **you** see and hear.

## Server settings

```
/function ra:settings
```

Needs permission level 2 (operator). The first page is a row of buttons, one per module; each
opens a page with buttons beside every value: step it up or down, or **edit** to type an exact
number.

Every setting is also a command, so you can tab-complete your way through them:

```
/function ra_settings:admin/wires/generator_eu_tick/up
/function ra_settings:admin/wires/generator_eu_tick/edit       (type an exact value)
/function ra_settings:admin/wires/electric_furnace/disable
```

### Typing a value

**edit** asks for the value in one of two ways:

- **A number** is typed with `/trigger ra.input.trigger set <n>` (the chat shows a ready-made
  link). The prompt waits 30 seconds.
- **Text** is written on page 1 of an **Input Form** book; close the book to submit. You may
  hold the form in either hand. The prompt waits 5 minutes, and the wait starts again whenever
  you hold the form, so it never runs out while you are writing. Dropping the form, or putting it
  in a chest, bundle or lectern, cancels the prompt and the form disappears.

### What you can change

| Page | Settings |
| --- | --- |
| General | show the load message; chat prefix (`RA`); Goggles range (16 blocks); Goggles refresh (20 ticks) |
| Logic Gates | default Clock interval (20), Delayer delay (20), Extender duration (20), Shortener pulse (2), Randomizer chance (50%) |
| Interactive | default Magic Crate radius (8) and interval (20), Message Block range (16) |
| Power & Fluids | default Generator EU/tick (60), Consumer interval (20), Industrial Light EU use (10), Liquid Drain interval (20) |
| Ender Links | default Power Vault EU/tick (80), Fluid Vault transfer (200) |
| Infinite Generators | default interval of each generator (100) |
| Jetpacks | Thruster Kit thrust (80%), speed cap (350), dead zone (25) |
| Enchant Crafting | enchant crafting on/off |
| every module | which of its blocks may be placed |

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

!!! warning "Access stays until you revoke it"
    The tag survives reloads and is **not** removed when someone loses operator status. Use
    `/function ra_settings:admin/revoke` (or `/tag <player> remove ra.admin`).

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
