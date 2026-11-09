# Displays

Blocks that show something in the world with display entities: a signal level as a bar, an item
floating above its block.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |

Operators: `/function ra_displays:items/give_all`.

---

## Advanced

- Every display is a `block_display`, `item_display` or `text_display` standing on the block's
  marker, tagged `ra` and `ra.display`, and moved where it is drawn by its transformation. Breaking
  the block removes them; so do the uninstall and the admin purge.
- Displays are fully lit (`brightness` 15): they stand inside the block, where the light is 0.
- One slow clock, `ra_displays:poll/run`, runs once a second while a display block that needs it is
  loaded, and stops when none is. It puts back a display killed by hand.
