# Storage

The `ra_storage` module adds Boxer, Unboxer, and Item Crate workflows for compact inventory transport.

- Namespace: `ra_storage`
- Give all: `/function ra_storage:items/give_all`
- Give crate only: `/function ra_storage:items/give_storage_box`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Summary

| Block | Item model | Recipe | Default I/O |
|---|---|---|---|
| Boxer | `minecraft:dropper` | ![Boxer recipe](images/recipes/ra_storage/boxer.png){ width="220" } | `input1="^ ^ ^-1"`, `output1="^ ^ ^1"` |
| Unboxer | `minecraft:dispenser` | ![Unboxer recipe](images/recipes/ra_storage/unboxer.png){ width="220" } | `input1="~ ~ ~"`, `output1="^ ^ ^1"` |

## Item Crate Model

Item Crates are craftable storage items (`storage_box.json`) and can also be given directly.
Crafting them is useless. They are made when you power a boxer block. And are emptied when you power an unboxer

- Base item: `minecraft:player_head` with profile `BoxMan01234`
- Stack size: `64`
- Storage payload keys:
  - `components.minecraft:custom_data.ra.storage_box.items`
  - `components.minecraft:custom_data.ra.storage_box.preview`
- Marker key for modern crates: `components.minecraft:custom_data.ra.item_box`
- Lore displays the first five preview lines; if more exist, lore appends `... and more`

## Two-Minute Setup

1. Place a Boxer and point its front toward an output container.
2. Put the source container behind the Boxer (default `input1`) and ensure output has free space.
3. Power the Boxer. It packs the full input container into one Item Crate and inserts that crate into `output1`.
4. Place an Unboxer facing an output container.
5. Put one or more Item Crates into the Unboxer inventory.
6. Power the Unboxer. It forwards one stored stack at a time from each crate into `output1`.

## Runtime Behavior

### Boxer

1. Uses redstone power detection and runs only while powered.
2. Requires both `input1` and `output1` to be valid `#ra_lib:containers`.
3. Copies the full `Items` list from `input1` into one Item Crate (`ra.storage_box.items`).
4. Builds a preview list and refreshes crate lore.
5. Inserts the generated crate into `output1` using shared item-mover capacity checks.
6. Clears input container contents only after successful insertion.
7. Enforces a hopper-like minimum 4 tick cooldown between successful operations.

### Unboxer

1. Uses redstone power detection and runs only while powered.
2. Selects one candidate item from `input1` (including partner chest handling when applicable).
3. Accepts both modern crates (`ra.item_box`) and legacy crates (`ra.storage_box_item`).
4. Moves the first stored stack from the crate into `output1`.
5. Supports partial insertion: only the inserted amount is removed from the stored stack.
6. Rebuilds crate preview and lore after each extraction.
7. Enforces a hopper-like minimum 4 tick cooldown between successful operations.

## Command Quick Reference

| Command | Purpose |
|---|---|
| `/function ra_storage:items/give_all` | Give Boxer, Unboxer, and Item Crate |
| `/function ra_storage:items/give_storage_box` | Give one empty Item Crate |
| `/function ra:items/bundles/give_storage_bundle` | Give a prefilled storage bundle |

## Troubleshooting

- Boxer does nothing: verify it is powered and both `input1`/`output1` point to valid containers.
- Boxer does not clear input: output likely cannot accept another item (full container).
- Unboxer does nothing: ensure the selected input item is an Item Crate with at least one stored stack.
- Unboxer stalls intermittently: check output capacity; partial insertion can leave a reduced first stack in the crate.

## Contributor Notes

1. Keep modern (`ra.item_box`) and legacy (`ra.storage_box_item`) crate compatibility paths intact.
2. Preserve partial-insert semantics in unboxing (`#mover_inserted` amount consumption).
3. If lore or preview format changes, update both `update_lore` and `update_lore_storage_target`.

---
