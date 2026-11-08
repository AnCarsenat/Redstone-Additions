# Tools

Five tools for setting up and checking your machines. Each does one job: the **Wrench** changes
blocks, the **Goggles** show what they are doing, the **Data Handler** edits their settings in
detail, the **Clipboard** copies settings between blocks, and the **Multimeter** reads networks.

## Getting them

| Tool | Looks like | Recipe |
| --- | --- | --- |
| Wrench | an orange rod | ![Wrench recipe](images/recipes/ra/wrench.png){ width="200" } |
| Goggles | an orange leather helmet | ![Goggles recipe](images/recipes/ra/goggles.png){ width="200" } |
| Data Handler | a light blue rod | ![Data Handler recipe](images/recipes/ra/data_handler.png){ width="200" } |
| Clipboard | paper | ![Clipboard recipe](images/recipes/ra/clipboard.png){ width="200" } |
| Multimeter | a clock | ![Multimeter recipe](images/recipes/ra/multimeter.png){ width="200" } |

Tools do not stack: each sits in its own slot.

Operators: `/function ra:give_all_items` gives everything in the pack; each tool also has its own
give function (`/function ra:tools/wrench/give`, `ra:tools/goggles/give`,
`ra:tools/data_handler/give`, `ra:tools/clipboard/give`, `ra:tools/multimeter/give`).

## Wrench

| Action | Does |
| --- | --- |
| Shift + right-click a block | Cycles its settings |
| Right-click a Wireless Emitter or Receiver | Turns it on or off |
| Right-click a Multiblock Base | Assembles the machine built around it ([Multiblocks](multiblocks.md)) |

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
| Liquid Drain | mode, rate |
| Electric Furnace | output face, power mode |
| EU Consumer | draw (20 / 40 / 80 EU per tick) |
| Creative Fluid Source | fluid |
| Ender Item / Fluid / Power Vault | mode |

## Goggles

Wear them or hold them. Every Redstone Additions block within 16 blocks shows a label with its
name and live status, refreshed once a second. A value the block has not worked out yet shows
`N/A` in red.

The Goggles only read; use the Wrench or the Data Handler to change things. Server operators can
change the range and refresh rate in [Settings](settings.md).

## Data Handler

Shift + right-click a block to open its settings in chat.

| Button | Does |
| --- | --- |
| **[Modify]** | Change a number or text: you get a book to write the new value in |
| **[Toggle]** | Switch an on/off setting |
| **[Edit]** / **[Remove]** | On each entry of a list: type a new value for it, or delete it |
| **[Clear]** | On a list whose positions matter (a Teleport Anchor's strengths): empty that entry, the others stay put |
| **[Append]** | Add an entry at the end of a list (greyed out when the list is full) |
| **[Choose]** | Pick an entry from a list of known values (a Teleport Anchor's destinations: every anchor named so far) |
| **[Edit whole list]** | Write the whole list at once, like `["A","B","C"]` |
| **[Set from hand]** | For item settings (an Item Pipe filter): use the item you are holding |
| **[Cancel Input]** | Abandon an edit you started |
| **[Refresh]** | Redraw the menu with current values |

Each kind of block declares its settings and what type each one is, so the Handler always offers
the right editor: a number prompt (with the allowed range) for numbers, **[Toggle]** for on/off
settings, the book for text, one line per entry for lists. What you type is checked before it is
written: a number field refuses `abc` or a value outside its range (the prompt shows the range,
e.g. *Enter a number for delay (1–1200)*), an on/off field accepts `true`/`false`, `on`/`off` or
`1`/`0`, and a text field keeps exactly what you wrote, even if it looks like a number. A list
entry must be of the list's type. A refused value leaves the block unchanged and says why.

A list looks like this (a Teleport Anchor):

```
  targets: list (15/15)  [Edit whole list]
    Strength 1: Mine  [Choose] [Edit] [Clear]
    Strength 2: (empty)  [Choose] [Edit]
```

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
- Goggles labels are `text_display` entities tagged `ra.billboard`.
