# Sensors

The `ra_sensors` module provides entity detection and tag mutation blocks.

- Namespace: `ra_sensors`
- Give all: `/function ra_sensors:items/give_all`
- Runtime architecture: [How It Works](how-it-works.md)

## Block Summary

| Block | Item model | Recipe | Trigger model | Property model |
|---|---|---|---|---|
| Entity Detector | `minecraft:waxed_chiseled_copper` | ![Entity Detector recipe](images/recipes/ra_sensors/entity_detector.png){ width="220" } | Continuous | `entity_selector` |
| Tag Adder | `minecraft:green_glazed_terracotta` | ![Tag Adder recipe](images/recipes/ra_sensors/tag_adder.png){ width="220" } | Rising edge | `tag`, `entity_selector` |
| Tag Remover | `minecraft:red_glazed_terracotta` | ![Tag Remover recipe](images/recipes/ra_sensors/tag_remover.png){ width="220" } | Rising edge | `tag`, `entity_selector` |

## Entity Detector

Entity Detector runs every tick and evaluates a configurable selector.

- Default selector at placement: `@e[type=minecraft:pig,distance=..5]`
- Uses macro dispatch through `detect_macro`.
- Output state is reflected by redstone shell swap (iron/redstone block shell logic).

## Tag Adder

Tag Adder applies tags only when power rises from off to on.

- Tick includes `ra_lib:redstone/detect`.
- Rising edge condition: `tag=ra.powered` and `tag=!ra.was_powered`.
- Uses macro expansion for dynamic selector and tag values.

## Tag Remover

Tag Remover also runs on rising edge.

- If `data.properties.tag` is empty or missing, it runs clear-all behavior for matched entities.
- Otherwise it removes only the configured tag.

## Practical Use

- Build trigger zones by pairing Tag Adder and Tag Remover with plates or detector lines.
- Use Entity Detector for map events by setting selector strings directly.
- Keep selectors targeted to avoid expensive global scans.

## Contributor Notes

1. Treat selector strings as user input: defensive checks prevent expensive scans.
2. Keep sensor operations edge-triggered when mutation is destructive (tag add/remove).
3. Expose editable fields through CDH and visible outputs through goggles when possible.

---
