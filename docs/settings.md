# Settings

Redstone Additions has two kinds of settings, and they are reached in two
different ways on purpose.

| | Server settings | Your settings |
|---|---|---|
| Who | Operators | Everyone |
| Reached with | `/function ra_settings:admin/show` | `/trigger ra.settings.open` |
| Affects | The whole world | Only you |
| Examples | Generator EU/tick, which blocks may be placed | Machine sounds, particles |

Both are printed once when the pack loads. The server one is a button. The player
one is deliberately **not** a button — clicking it puts the command in your chat
box instead of running it, because it is the only way back into the menu after
that message has scrolled away, and a button that silently works teaches nobody
its name.

---

## Your settings

```
/trigger ra.settings.open
```

No arguments, no permissions. `/trigger` with no value adds one, which is exactly
what the menu treats as "open".

| Setting | Default | What it does |
|---|---|---|
| Machine sounds | on | Every `playsound` the pack makes is filtered on this |
| Machine particles | on | Every `particle` the pack makes is filtered on this |
| Debug messages | off | Registration and diagnostic chat, via the `ra.debug` tag |

These are yours. Two players standing at the same wall of machines can disagree
about how loud it is.

!!! note "Why there is no 'mute chat' switch"
    Most of the pack's chat is a direct answer to something you just clicked — a
    wrench menu, a settings confirmation, an error. Muting those breaks the tools,
    so the switch that exists is the one that can be honoured: debug messages.

---

## Server settings

```
/function ra_settings:admin/show
```

Requires permission level 2. The index is a row of buttons, one per module; each
opens a page where every value has buttons beside it.

Everything also autocompletes. Type `/function ra_settings:admin/` and press tab
to walk the whole tree — that is the reason these are functions and not a menu.

```
/function ra_settings:admin/wires/generator_eu_tick/up
/function ra_settings:admin/wires/generator_eu_tick/edit     ← type an exact value
/function ra_settings:admin/wires/electric_furnace/disable
```

### `ra.admin`, and why the buttons stopped asking

The buttons on every page fire a trigger rather than running their function. That
is what stops Minecraft asking you to confirm each click — a `run_command` link
prompts every time, a `/trigger` does not.

A trigger does not check permissions, so the check is the **`ra.admin` tag**.
Anyone holding it opens the panel straight from the button and can press
everything on it. Anyone without it is handed the command unrun instead.

```
/function ra_settings:admin/grant     give the nearest player access
/function ra_settings:admin/revoke    take it back
```

Opening the index as an operator also grants it, so in practice you never have to
run `grant` yourself.

The tag **persists across reloads** — that is the point of tagging somebody. It
can only be handed out by something that already needs permission level 2 (`/tag`,
or either function above), so no player can give it to themselves.

!!! warning "It is a role, not a session"
    Because it persists, removing somebody's operator status does **not** remove
    their settings access. Use `/function ra_settings:admin/revoke`, or
    `/tag <player> remove ra.admin`, when you take someone's admin rights away.

### Turning blocks off

Every placeable block has an `enable` / `disable` pair. A disabled block cannot be
placed, and the item is handed back rather than swallowed — the policy is the
admin's, and it should not cost the player their block.

**Disabling never removes blocks already in the world.** They keep working. An
admin who wants them gone can break them.

### Changing defaults

Rows marked *(new blocks only)* set the value a block is given **when it is
placed**. Machines already standing keep what they have.

That is deliberate. The alternative — reading the setting live — would put a
lookup in `ra_lib:util/property`, which runs for every consumer, every bridge and
every drain on every tick. It would also silently re-tune a build that was
balanced around the old number. Wrench an existing block if you want it changed.

### Typing exact values

Stepping is one click, but moving a generator from 60 to 500 EU is forty-four of
them. Every numeric setting also has `edit`, which opens the same input form the
Data Handler uses for a clock's delay.

---

---

## Uninstalling

```
/function ra:uninstall
```

Also a button at the bottom of the server settings index.

It asks **twice**. The first prompt asks; the second lists exactly what is about
to be destroyed — every machine becomes an ordinary block, every network,
multiblock and display is removed, and every setting including your
disabled-block list is erased.

Both confirmations deliberately use ordinary command links rather than the
triggers the rest of the panel uses, so Minecraft's own "run this command?"
dialog stays in the way. This is the one place the extra friction is worth having.

There is no backup. Copy the world first if you want one.

---

## Adding a setting

One JSON file per page, in `tools/settings/`. The build generates the menu, the
defaults, the per-player seeding and the whole operator function tree from it —
there is no list anywhere to keep in step.

```json
{
  "id": "wires",
  "title": "Power & Fluids",
  "namespace": "ra_wires",
  "rows": [
    {"type": "prop", "block": "electric_generator", "prop": "generation_rate",
     "label": "Generator EU/tick", "default": 60, "min": 1, "max": 10000, "step": 10},
    {"type": "block", "block": "electric_furnace", "label": "Electric Furnace"}
  ]
}
```

| Row type | Meaning |
|---|---|
| `bool` | On/off |
| `int` | A number, stepped or typed |
| `str` | Text, typed |
| `list` | A cycle through fixed choices; stores the **index**, so renaming a choice orphans nothing |
| `block` | Whether a block may be placed |
| `prop` | The default a newly placed block is given |

`scope` is `global` or `user`, and it decides how the setting is reached, not just
who may change it. A `global` row generates the operator function tree and never
appears in the player menu. A `user` row appears only in the player menu, and
needs an `obj` — a short, stable scoreboard objective name.

!!! warning "`obj` is the setting's identity"
    A player's saved choice is found by objective name. Renaming one silently
    resets it for everybody. Keep it under 16 characters and do not change it.

### Reading a setting

```mcfunction
function ra_settings:get     {key:"welcome"}
function ra_settings:prop    {block:"electric_generator",prop:"generation_rate",default:60}
function ra_settings:user    {obj:"ra.u.snd",default:1}
function ra_settings:enabled {block:"electric_furnace"}
```

The first three leave the answer in `#setting ra.set.tmp` and also return it.
`enabled` returns 1 when the block may be placed.

`ra_settings:user` must run as the player. An absent score is not zero — it means
the player has never chosen, so the default stands.
