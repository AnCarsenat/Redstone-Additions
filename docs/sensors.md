# Sensors

Blocks that react to entities: one detects them, two add or remove scoreboard tags on them. Handy
for adventure maps, mob farms and trigger zones.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |
| Entity Detector | waxed chiseled copper | ![Entity Detector recipe](images/recipes/ra_sensors/entity_detector.png){ width="220" } |
| Tag Adder | green glazed terracotta | ![Tag Adder recipe](images/recipes/ra_sensors/tag_adder.png){ width="220" } |
| Tag Remover | red glazed terracotta | ![Tag Remover recipe](images/recipes/ra_sensors/tag_remover.png){ width="220" } |

Operators: `/function ra_sensors:items/give_all`.

## Using them

All three are configured with a **target selector** (`entity_selector`), the same syntax as
commands: `@e[type=zombie,distance=..8]`, `@a[tag=red_team]`, … Edit it with the
[Data Handler](tools.md).

!!! note "Admins only, unless the server allows it"
    A selector can reach any entity in the world and a Tag Adder can hand any tag to anyone, so
    selectors and tags are changed by players with server-settings access. An operator can let
    everyone edit them with **Players may edit entity selectors and tags** on the Sensors page of
    the [server settings](settings.md).

What they accept:

- **Selector:** `@a`, `@e`, `@n`, `@p`, `@r` or `@s`, optionally followed by one `[...]`. Spaces
  only inside the brackets, quotes only as `"..."` (for `name="Bob Smith"`), no backslash, and
  nothing after the closing `]`.
- **Tag:** letters, digits and `_ . + -`, no spaces, and never starting with `ra.` (the pack's own
  tags, which include server-settings access).

A value that does not fit is refused when you type it, and a block holding one (from an older
world, or written with a command) does nothing until it is corrected: the Entity Detector stays
off, the Tag Adder and Tag Remover do not fire.

| Block | When | Does | Default settings |
| --- | --- | --- | --- |
| Entity Detector | every tick | Output is on while at least one entity matches the selector | `@e[type=minecraft:pig,distance=..5]` |
| Tag Adder | when it gets powered | Adds every tag in `tags` to every matching entity | `@e[type=pig,distance=..5]`, tags `["custom_tag"]` |
| Tag Remover | when it gets powered | Removes every tag in `tags` from every matching entity — or **all** of their tags if `tags` is an empty list (never the pack's own `ra.` tags, and never on the pack's own entities) | `@e[distance=..3]`, tags `["custom_tag"]` |

### The tag list

The Tag Adder and Tag Remover each take a list of up to 16 tags, `tags`. In the
[Data Handler](tools.md) it is shown one entry per line: **[Edit]** changes an entry,
**[Remove]** deletes it, **[Append]** adds one at the end, and **[Edit whole list]** takes the
whole list at once, written like `["red_team","in_zone"]`. Like the selector, the list is changed
by admins only unless *Players may edit entity selectors and tags* is on.

| `tags` | Tag Adder | Tag Remover |
| --- | --- | --- |
| `["a","b"]` | adds `a` and `b` | removes `a` and `b`, leaves every other tag |
| `[]` (empty) | does nothing | clears **every** tag except the `ra.` ones |

Each tag is checked again every time the block fires. A tag that is not allowed (`ra.admin`, one
with a space, …) is skipped and the others still apply; the Goggles then show **Refused tags: N**
until the list is fixed. An empty entry (`""`) is skipped without a warning. A list whose entries
are all refused does nothing: it is never read as "clear them all", only a truly empty list is.

The Goggles show the selector (**Targets**) and the first three tags (**Tags**, then "+N more").

!!! note "Blocks from before v6.0.2"
    Older Tag Adders and Tag Removers have a single `tag` instead of a list, and keep working
    exactly as before (an empty `tag` on a Tag Remover still clears every tag). Their Goggles
    show **Tag**. As soon as you give one a `tags` list (with **[Append]** or
    **[Edit whole list]**), the list is used and the old `tag` is ignored. Admins still see the
    old `tag` in the Data Handler, greyed as hidden from players.

**Detector output:** put **iron blocks** next to the Entity Detector (anywhere in the 3×3×3 cube
around it). They turn into **redstone blocks** while it detects something.

Tag Adder and Tag Remover act once per power-on (a button press or a pulse), not continuously.
The Entity Detector never counts Redstone Additions' own machines.

!!! tip "Keep selectors small"
    A selector with a `distance` or `type` filter is cheap. A bare `@e` checks every entity in
    the world every tick.

## Ideas

- A pressure plate into a Tag Adder marks players who entered a zone; a Tag Remover at the exit
  clears it.
- An Entity Detector set to `@e[type=item,distance=..2]` turns on when something is dropped on it.

---

## Advanced

- Properties on the marker: `data.properties.entity_selector`, and on the Tag Adder / Remover
  `data.properties.tags` (a list; `data.properties.tag` on blocks from before v6.0.2, read only
  while `tags` is absent). `data.status.skipped` holds the number of refused tags from the last
  pulse, and is removed when none were refused.
- The selector and the tag are pasted into macro commands
  (`ra_sensors:blocks/entity_detector/detect_macro`, `…/tag_adder/add_tag_macro`, …). Before each
  use, `ra_lib:safe/guard` checks them (formats `SELECTOR` and `TAG`, see
  [Tools and Registries](tools-and-registries.md#text-formats)); the value last found safe is
  cached in `data.data.safe.<name>`, so an unchanged value costs one comparison. Each entry of
  `tags` is checked with `ra_lib:safe/check` (format `TAG`) on every pulse, and only the entries
  that pass are pasted into `tag @s add|remove $(t)` (`ra_sensors:tags/*`). The selector is
  evaluated once per pulse; each matched entity then gets the whole list.
- Tag Adder / Remover fire on a rising edge: `ra.powered` without `ra.was_powered`, from
  `ra_lib:redstone/detect`.
