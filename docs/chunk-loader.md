# Chunk Loader

The `ra_chunk_loader` module contains one block that force-loads its chunk while powered.

- Namespace: `ra_chunk_loader`
- Give all: `/function ra_chunk_loader:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Summary

| Block | Item model | Physical block | Recipe |
|---|---|---|---|
| Chunk Loader | `minecraft:lodestone` | `minecraft:lodestone` | ![Chunk Loader recipe](images/recipes/ra_chunk_loader/chunk_loader.png){ width="220" } |

## Runtime Behavior

Chunk Loader tick logic:

1. Break detection verifies the lodestone still exists.
2. Redstone state is read with `ra_lib:redstone/detect`.
3. Rising edge (`!ra.was_powered` and powered) runs `forceload add ~ ~`.
4. Falling edge (`ra.was_powered` and unpowered) runs `forceload remove ~ ~`.

State tags used:

- `ra.custom_block.chunk_loader`
- `ra.was_powered`

## Usage Notes

- Place one per chunk you want controlled.
- Power on to lock the chunk loaded.
- Power off to release it.
- Recommended for machine hubs and long-distance automation relays.

## Operational Caution

Because this block writes to forceload state, treat it as infrastructure:

- Document where loaders are placed.
- Clean up abandoned loaders in old farms.
- Use `/forceload query` during diagnostics.

## Contributor Notes

1. Keep edge-detection logic strict to avoid repeated `forceload` churn.
2. Preserve on-break and unpowered cleanup paths to prevent stuck forced chunks.
3. Test loader behavior across reloads and power flicker conditions.

---
