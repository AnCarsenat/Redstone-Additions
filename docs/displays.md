# Displays

Blocks that show something in the world with display entities: a signal level as a bar, an item
floating above its block, a name or a number in glowing tube-style text.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |
| Gauge Bar | polished blackstone | ![Gauge Bar recipe](images/recipes/ra_displays/gauge_bar.png){ width="220" } |
| Item Hologram | barrel, lid up | ![Item Hologram recipe](images/recipes/ra_displays/item_hologram.png){ width="220" } |
| Text Screen | barrel, a dark screen on its lid | ![Text Screen recipe](images/recipes/ra_displays/text_screen.png){ width="220" } |

Operators: `/function ra_displays:items/give_all`.

## Gauge Bar

A bar on the front of the block, filled to the redstone level feeding it: empty at 0, full at 15.

1. Place it: the bar is drawn on the face looking at you (it can face up or down too).
2. Feed it redstone from any side. The strongest level wins; a repeater or comparator pointing
   into it fills it completely. Dust carries its real level: to show how full a chest is, put one
   dust between the comparator reading it and the Gauge Bar.
3. Shift+right-click with the Wrench to change the colour or turn the bar vertical.

| Property | Values | Default |
| --- | --- | --- |
| `color` | red, orange, yellow, lime, green, cyan, light_blue, blue, purple, magenta, pink, white (anything else draws red) | red |
| `vertical` | on: the bar fills bottom to top; off: left to right | off |

The Goggles show the level, the colour, the orientation and the facing. A colour typed with the
Data Handler shows within a second.

Crafted from three glass panes, two redstone, a comparator and three polished blackstone.

## Item Hologram

A barrel that shows the item inside it floating half a block above its lid, turning slowly, with
the item's name over it.

1. Place it and put an item in (by hand, a hopper, an Item Pipe...). The first item in the barrel
   is the one shown; how many there are does not matter.
2. Take it out, or put another one first, and the hologram follows within a second.
3. A renamed item shows its anvil name; any other its usual name, in your language.

| Property | Values | Default |
| --- | --- | --- |
| `spin` | the item turns (a full turn every 4 seconds) | on |
| `show_name` | the item's name floats above it | on |

Shift+right-click with the Wrench to turn either off. The display is the same for everyone looking
at it, so these belong to the block, not to each player. Breaking it spills the barrel's contents
as any barrel does.

Crafted from a glass block, two amethyst shards, an ender pearl and a barrel.

## Text Screen

A barrel with a dark screen on its lid, showing one line of glowing amber text like a nixie tube.
The redstone level feeding it picks what it shows:

| Level | Shows |
| --- | --- |
| 15 | the name of the item in the barrel's first slot |
| 14 | the second slot |
| ... | ... |
| 1 | the fifteenth slot |
| 0, or the chosen slot empty | nothing: the tube is dark |
| any, barrel empty | the level itself, 0 to 15, in large digits |

1. Place it: the screen is on the face looking at you (it can face up or down too).
2. Put items in its first fifteen slots, one per name to show: a renamed item shows its anvil
   name, any other its usual name, in your language. Long names wrap onto a second line.
3. Feed it redstone from any side. The strongest level wins; a repeater or comparator pointing
   into it reads as 15. A dust line from a Sequencer, a Signal Calculator or a comparator turns
   it into a small message board.

The screen changes as soon as the level does; a change to the barrel's contents shows within a
second. Breaking it spills the barrel's contents as any barrel does. The Goggles show the level
and the facing. It has no properties.

Crafted from three glass panes, two glowstone dust, a comparator and a barrel.

---

## Advanced

- Every display is a `block_display`, `item_display` or `text_display` standing on the block's
  marker, tagged `ra` and `ra.display`, and moved where it is drawn by its transformation. Breaking
  the block removes them; so do the uninstall and the admin purge.
- Displays are fully lit (`brightness` 15): they stand inside the block, where the light is 0.
- One slow clock, `ra_displays:poll/run`, runs once a second while a display block that needs it is
  loaded, and stops when none is. It puts back a display killed by hand.
- Gauge Bar: marker tag `ra.custom_block.gauge_bar`, displays `ra.displays.gauge_track` and
  `ra.displays.gauge_fill`. The input is read every tick (each side asked `any_side` first, as the
  gates do); the fill is redrawn only when the level changes, and the client eases it over 3 ticks.
  The last level drawn is the marker's `ra.power` score; `data.status.level` holds it for the Goggles.
- Item Hologram: marker tag `ra.custom_block.item_hologram`, displays `ra.displays.holo_item` and
  `ra.displays.holo_name`. Nothing runs per tick: once a second it compares `Items[0]` (without
  slot and count) with `data.data.shown` and redraws only on a difference. The spin is the client's:
  each second sets the next 90-degree step with a 20-tick interpolation. The name is copied as a
  component (`custom_name`, else `item_name`), or a translation built from the item id
  (`ra_displays:item_name/build`, shared with the Text Screen).
- Text Screen: marker tag `ra.custom_block.text_screen`, displays `ra.displays.screen_panel` (a
  black concrete `block_display`) and `ra.displays.screen_text`. The input is read every tick like
  the Gauge Bar's, the last level in `ra.power` (`data.status.level` for the Goggles). What it
  would show (`{item:...}` without slot and count, `{level:L}`, or `{}`) is built when the level
  changes and once a second on the slow clock, and compared with `data.data.shown`: the text is
  only rewritten on a difference.
