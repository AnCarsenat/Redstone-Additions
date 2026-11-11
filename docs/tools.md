# Tools

Five tools for setting up and checking your machines. Each does one job: the **Wrench** changes
blocks, the **Goggles** show what they are doing, the **Data Handler** edits their settings in
detail, the **Clipboard** copies settings between blocks, and the **Multimeter** reads networks.

## Getting them

| Tool | Looks like | Recipe |
| --- | --- | --- |
| Wrench | an orange rod | ![Wrench recipe](images/recipes/ra/wrench.png){ width="200" } |
| Goggles | an orange leather helmet | ![Goggles recipe](images/recipes/ra/goggles.png){ width="200" } |
| Goggles Lens | a spyglass | ![Goggles Lens recipe](images/recipes/ra/goggles_lens.png){ width="200" } |
| Data Handler | a light blue rod | ![Data Handler recipe](images/recipes/ra/data_handler.png){ width="200" } |
| Clipboard | paper | ![Clipboard recipe](images/recipes/ra/clipboard.png){ width="200" } |
| Multimeter | a clock | ![Multimeter recipe](images/recipes/ra/multimeter.png){ width="200" } |

Tools do not stack: each sits in its own slot.

Operators: `/function ra:give_all_items` gives everything in the pack; each tool also has its own
give function (`/function ra:tools/wrench/give`, `ra:tools/goggles/give`,
`ra:tools/data_handler/give`, `ra:tools/clipboard/give`, `ra:tools/multimeter/give`,
`ra:tools/goggles/lens/give`).

## Wrench

| Action | Does |
| --- | --- |
| Shift + right-click a block | Cycles its settings |
| Shift + right-click a minecart with a Ram Upgrade | Takes the upgrade off and gives it back ([Railworks](railworks.md#ram-upgrade)) |
| Right-click a Multiblock Base | Assembles the machine built around it ([Multiblocks](multiblocks.md)); on one already assembled it only says so |

What Shift + right-click does depends on the block:

- **No settings** — it says so.
- **One setting** — it changes straight away.
- **Several settings** — a menu in chat lists each one with its current value and a
  `[ CYCLE ]` button. The menu stays tied to that block, so you can look away while clicking.

```
── electric_furnace ──
  Output: [under]   [ CYCLE ]
  Power: [low]      [ CYCLE ]
```

| Block | Wrench settings |
| --- | --- |
| UNI Gate | gate type |
| Electric Furnace | output face, power mode |
| EU Consumer | draw (20 / 40 / 80 EU per tick) |
| Creative Fluid Source | fluid |
| Ender Item / Fluid / Power Vault | mode |
| Weather Sensor | mode (rain / thunder / day / night) |
| Held Item Detector | slot (mainhand / offhand / either / inventory) |
| Gauge Bar | facing, direction, colour, track colour |
| Item Hologram | spin, name, facing, direction, name colour |
| Text Screen | facing, direction, colour |

## Goggles

Wear them or hold them. Every Redstone Additions block within 16 blocks shows a label with its
name and live status, refreshed once a second. A value the block has not worked out yet shows
`N/A` in red.

The label sits just above the block and grows upward with what it has to say: a block with one
line gets a small label close to its top, one with five a taller one from the same spot. An
Item Pipe's filter item floats just above its label.

You can turn that floating item off for yourself with **Show filter previews** in your own
settings (`/trigger ra.settings.open`), and server operators can turn it off for everyone
(*Filter previews*, General › Goggles). A label is a single entity that everyone nearby sees, so
it cannot be hidden from one player only: a pipe draws its filter item while **any** player
wearing Goggles within range of it has *Show filter previews* on. Turning yours off stops you
asking for previews; you still see one that is drawn for another Goggles wearer near the same
pipe.

A label shows what the block is doing and the settings worth knowing at a glance. Settings the
Data Handler hides from players are not shown, and a few fixed or uninteresting values (a
generator's cooldown, the Magic Crate's pulse cooldown) are left off the label; the Data Handler
still lists them.

The Goggles only read; use the Wrench or the Data Handler to change things. Server operators can
change the range and refresh rate in [Settings](settings.md).

### Goggles Lens

An expensive upgrade that gives the Goggles' view to a helmet of your own, so you need not choose
between your armour and the labels. Crafted from four tinted glass, two diamonds, two gold ingots
and a netherite ingot.

1. Wear the helmet: leather, copper, chainmail, iron, gold, diamond, netherite or a turtle shell.
2. Hold the lens and right-click. The lens is used up.

The helmet keeps its material, enchantments, trims and durability, and now shows every label the
Goggles show, worn or held. Any helmet takes the lens, including one with a name, a description
(lore) or another pack's data; all of it is kept. The lens adds its own lines at the top of the
lore, saying whether it is on. The Goggles themselves, and a helmet that already has a lens, are
refused.

`/trigger ra.goggles` opens the lens menu for the helmet you are wearing:

```
── Goggles Lens ──
  Lens  [ ON ]  [ REMOVE ]
```

- **[ ON ] / [ OFF ]**: switch the lens off without removing it, and back on. The switch is stored
  on the helmet, so it stays off when you hand the helmet to someone else.
- **[ REMOVE ]**: take the lens out and get it back as an item; only the lens's lines and data
  are taken off, so the helmet is as it was before.

## Data Handler

Shift + right-click a block to open its settings in chat.

| Button | Does |
| --- | --- |
| **[Modify]** | Change a number or text: you get a book to write the new value in |
| **[Toggle]** | Switch an on/off setting |
| **[Edit]** / **[Remove]** | On each entry of a list: type a new value for it, or delete it |
| **[Clear]** | On a list whose positions matter (a Teleport Anchor's strengths): empty that entry, the others stay put |
| **[Add]** | Add an entry at the end of a list, also an empty one: you write it in the book, and it is checked like the others. Greyed out once the list is full ("Maximum N entries" on hover) |
| **[Edit all]** | Write the whole list at once, like `["A","B","C"]` |
| **[List]** | Show a list's entries under it, each with its own buttons; click again to hide them. One list is open at a time |
| **[Set from hand]** | For item settings (an Item Pipe filter): use the item you are holding |
| **[Edit]** | On a position or offset: show its editor under it; click again to hide it |
| **[Type]** / **[-X]** **[+X]** … | In a position's editor: type one axis, or move it by the chosen step |
| **[Pick]** | In a position's editor: set it to the block you are looking at |
| **[Reset]** | In a position's editor: back to the block's starting value |
| **[Clear]** | On item settings: set no item (removes an Item Pipe filter) |
| **[Cancel Input]** | Abandon an edit you started |
| **[Refresh]** | Redraw the menu with current values |

Each kind of block declares its settings and what type each one is, so the Handler always offers
the right editor: a number prompt (with the allowed range) for numbers, **[Toggle]** for on/off
settings, the book for text, one line per entry for lists. What you type is checked before it is
written: a number field refuses `abc` or a value outside its range (the prompt shows the range,
e.g. *Enter a number for delay (1–1200)*), an on/off field accepts `true`/`false`, `on`/`off` or
`1`/`0`, and a text field keeps exactly what you wrote, even if it looks like a number. A list
entry must be of the list's type. A refused value leaves the block unchanged and says why.

A list is one line until you open it with **[List]**. Opened, it looks like this (a Teleport
Anchor):

```
  targets: list (15/15) [Edit all] [List]
    Strength 1: Mine  [Edit] [Clear]
    Strength 2: (empty)  [Edit]
```

and like this for a list that grows (a Tag Adder's tags, at most 16), with its count in the header:

```
  tags: list (2/16) [Add] [Edit all] [List]
    Tag 1: custom_tag  [Edit] [Remove]
    Tag 2: red_team  [Edit] [Remove]
```

A position or an offset (a Block Detector's `target`, a display's `offset`) is three whole
numbers, x, y and z. It is one line until you open it with **[Edit]**:

```
  target: [x 0, y 1, z 0] vec3 (blocks, from the block) [Edit]
    X: 0  [Type] [-X] [+X]  (-16 to 16)
    Y: 1  [Type] [-Y] [+Y]  (-16 to 16)
    Z: 0  [Type] [-Z] [+Z]  (-16 to 16)
    Step: [1] [4] [16] (blocks)
    [Pick] [Reset]
```

- **[Type]** asks for one axis with the number prompt, which shows its range.
- **[-X]** / **[+X]** (and Y, Z) move it by the step chosen on the **Step** line (in gold). A move
  stops at the end of the range.
- **[Pick]** takes the block you are looking at, up to 24 blocks away: look at it, open chat, click.
  Not offered for a display's offset, which is on the display's own face.
- While the editor is open, and for 10 seconds after each change, the block a position points at
  is outlined in green. A display's offset needs no outline: the display moves there within a
  second.
- The range is the block's. Admins get a wider one where the block has one (the Block Detector:
  64 blocks instead of 16); a value outside the players' range is marked **(admin)**, and only an
  admin can change it.

A direction (a display's `facing`) is one line with a button per face; the current one is gold:

```
  facing: [south] direction [Down] [Up] [North] [South] [West] [East]
```

A facing (a display's `rotation`) is a yaw and a pitch in whole degrees. Open it with **[Edit]**:

```
  rotation: [yaw 0, pitch 0] facing (degrees, from the block's front) [Edit]
    Yaw: 0  [Type] [-] [+]  (-180 to 180)
    Pitch: 0  [Type] [-] [+]  (-90 to 90)
    Step: [1°] [15°] [45°]
    [Reset]
```

Yaw turns it clockwise seen from above and a positive pitch tips its front down, as a mob turns
its head. Yaw goes round: past 180 it comes back at -180. A compass facing also has
**[North] [East] [South] [West]**, which set the yaw and level the pitch.

A colour shows a swatch and its value (`#RRGGBB`, or `#AARRGGBB` when it has an opacity), or
*none*. Open it with **[Edit]**:

```
  background: [■ none] color (ARGB) [Edit]
    ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■
    Opacity: [100%] [75%] [50%] [25%]
    [Type] [None] [Reset]
```

- Each **■** is one of the sixteen chat colours (its name on hover). Picked from *none*, a
  background starts 80 % opaque; otherwise the opacity is kept.
- **Opacity** keeps the colour and changes how much shows through.
- **[Type]** takes a colour name (`red`, `gold`...), `none`, `#RRGGBB`, `#AARRGGBB` or a number.

Every button acts on the block the menu shows. If you selected another block since (with the
Creative Data Handler, say), a click in the old menu changes nothing and the menu is shown again
for the block selected now.

Selecting a block with the Handler shows every list closed again. A list you may not change
(set by the block, or admins only) keeps its locked row; its **[List]** still shows the entries,
without buttons.

Every setting the block has is listed, even one it does not carry yet because it was placed
before that setting existed. Such a row shows the setting's starting value marked *(default)*,
or *(not set)* when it has none, and your first change writes it to the block.

Text settings only accept the characters they can use, since some are used inside commands:
names and channels take letters, digits, spaces and `_ . + - : / #`; most other text the same
without spaces; entity selectors and tags have their own rules (see [Sensors](sensors.md)); a
Boxer's input and output are three relative coordinates within 9 blocks, like `^ ^ ^-1`. The
refusal says what is allowed.

Some values are the block's own working numbers (a generator's output rate, a valve's rate) and
cannot be changed: they show a crossed-out red `[Modify]`, with the reason when you hover it. A
server can also keep a setting for admins (players with server-settings access): other players
see it locked, and settings marked hidden do not appear for them at all. Some admin-only settings
can be opened to everyone by a server setting: entity selectors and tags, with *Players may edit
entity selectors and tags* (Sensors page).

Admin-only settings are marked with a gold **(admin)** after their name, for everyone: an admin
sees which of the things they can change are theirs alone, and other players see why the row is
locked. The Wrench's cycle menu marks them the same way for admins.

Admins also go past the maximum of a range or radius (a Big Torch's or Magic Crate's `radius`, a
Message Block's `range`), up to a higher technical ceiling: the number prompt shows the admin's
bounds. A value above the players' maximum is marked **(admin)** too (hover it), and the block
keeps it whoever uses it later. Other players can still lower it to a value within their range.
The Clipboard pastes a number, or a position, only within the range its holder could type.

A value no block declares (one added by an add-on or with the Creative Data Handler) still gets
a row when the Handler can tell its type from its value, marked with a yellow **⚠**; hover it to
see what was guessed. Anything it cannot place is listed under **Undeclared**.

A menu's buttons only work in the latest menu you were shown. Clicking one in an older menu (or
one from before a `/reload`) redraws the menu instead of changing anything.

If your inventory is full, the input book cannot be given — free a slot and try again.

## Clipboard

Copies settings from one block to others of the **same kind**.

1. Shift + right-click a block: it becomes the source.
2. Shift + right-click other blocks of the same kind: each gets the source's settings.
3. Shift + right-click at nothing to clear the source.

## Multimeter

Right-click a block to read its network in chat: which network it is on, what the network holds
and its capacity, what this block adds, and what it uses or makes. A pipe carrying several fluids
at once lists each one with its amount. Two blocks showing different network numbers are not
connected. See [Transport Networks](transport-networks.md).

---

## Advanced

- **Creative Data Handler** (`/function ra:tools/creative_data_handler/give`): edits anything,
  including locked values, adds and removes properties and shows a block's internal data. Meant
  for building the pack, not for play. Each operator has their own selected block, and the
  `/data` commands its buttons suggest name that block by number, so they still reach it when
  you finish typing later.
- Each block type's settings, their types and their flags (read-only, admin-only, hidden) are
  its **property schema**, written to `storage ra:dh types` at load. The Data Handler, the
  Wrench and the Clipboard all follow it. See
  [Tools and Registries](tools-and-registries.md#property-schemas) for the full rules, including
  how the Data Handler guesses the type of an undeclared value.
- The Clipboard copies only `data.properties`, never a block's internal state (`data.data`).
- The Data Handler's menu buttons work through `/trigger ra.dh.action` and `/trigger ra.edit_type`,
  the Wrench menu through `/trigger ra.wrench`; these triggers are only enabled while you hold the
  tool (and for a short while after).
- A Goggles Lens helmet that is switched on carries `custom_data` `{ra:{goggles:1b,lensed:1b}}`,
  the same `goggles` flag as the Goggles, so the Goggles check finds it at no extra cost; switched
  off it carries `{ra:{lensed:1b}}`. These keys are merged into any `custom_data` the helmet
  already has, and the lens lore lines sit at the top of the helmet's lore. The lens menu is `/trigger ra.goggles` (1 menu, 2 switch,
  3 remove), enabled for anyone wearing a lensed helmet.
- Goggles labels are `text_display` entities tagged `ra.billboard` (an item filter is an
  `item_display` with the same tag), one per block: the lines are joined into one text, drawn
  from 0.3 above the block's top face upward. During a redraw a block marker carries the tag
  `ra.goggles_fprev` when a Goggles wearer in range has `ra.u.fprev` (Show filter previews) at 1
  and the server's `goggles_filter_preview` is on; the Item Pipe draws its filter item only then.
  Which settings a label shows follows each
  setting's `goggles` flag, see [Property schemas](tools-and-registries.md#property-schemas).
