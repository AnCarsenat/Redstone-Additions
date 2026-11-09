# Automation

Blocks and items that run vanilla machines for you. The **Crafter Controller** fires a vanilla
crafter on its own once the crafter holds the ingredients its **Pattern** asks for.

## Getting them

| Item | Looks like | Recipe |
| --- | --- | --- |
| Crafter Controller | target block | ![Crafter Controller recipe](images/recipes/ra_automation/crafter_controller.png){ width="220" } |
| Pattern | flow banner pattern | ![Pattern recipe](images/recipes/ra_automation/pattern.png){ width="220" } |

Operators: `/function ra_automation:items/give_all` (a Crafter Controller and a blank Pattern),
`/function ra_automation:pattern/give` (one blank Pattern).

## Pattern

A Pattern remembers a 3×3 crafting grid: which item goes in each of the nine slots, or that a
slot stays empty. It ignores stack sizes and item details (names, enchantments), only the item
type counts.

| Do this, holding a Pattern | What happens |
| --- | --- |
| **Sneak + right-click a crafter** | Records the crafter's grid as it is now. Fill the grid by hand first, one of each ingredient is enough. |
| **Sneak + right-click air** (or any other block) | Clears the Pattern back to blank. |
| **Right-click a Crafter Controller** | Loads the Pattern into it (see below). |

Hover over a recorded Pattern to see its grid, row by row, as item names; `-` is an empty
slot. A crafter's disabled slots are recorded as empty.

## Crafter Controller

1. Place a vanilla **crafter** and fill its grid once by hand with the recipe you want.
2. Record it with a Pattern (sneak + right-click the crafter).
3. Place the **Crafter Controller** right against the crafter: under it, beside it or on top.
4. Right-click the Controller with the Pattern. The Controller keeps it; if it already held one,
   that one comes back to you.
5. Feed the crafter with hoppers, droppers or pipes as usual.

Whenever every slot of the crafter holds the Pattern's item (and every slot the Pattern leaves
empty is empty, or disabled), the Controller powers itself for an instant and the crafter
crafts once, exactly as if you had pulsed it with redstone: the result comes out of its front,
into a container if there is one. The Controller then waits until the crafter's contents
change before it looks again, so a crafter that cannot craft (a wrong recipe, an output that is
full) is not fired over and over.

- It looks every half second, so a crafter crafts at most twice a second.
- A crafter that is already powered by something else is left alone.
- With several crafters around it, it runs the first one it finds: above, then north, south,
  east, west, then below.
- The Controller is a target block and gives off a short redstone pulse when it fires: dust or
  other machines touching it see that pulse too.

### Wrench and tools

| Tool | Does |
| --- | --- |
| Wrench, Shift+RMB | Switches the Controller off and on (`enabled`) |
| Wrench, RMB | Takes the Pattern back out, dropped on top of the Controller |
| Goggles | State (waiting for the ingredients, crafting, crafter empty, no crafter next to it, …) and the number of crafts |
| Data Handler | `enabled`, and the loaded Pattern's nine ids (read-only) |

Breaking the Controller drops it and the Pattern it held.

## Troubleshooting

| Goggles say | Check |
| --- | --- |
| no pattern | Right-click it with a recorded Pattern |
| no crafter next to it | The crafter must touch the Controller face to face |
| waiting for the ingredients | Some slot holds a different item, or holds something where the Pattern has an empty slot |
| crafter powered | Something else keeps the crafter powered; it needs to be unpowered to be fired |
| crafting, but nothing comes out | The ingredients are not a real recipe, or the crafter's output is blocked |
| pattern unreadable | The Pattern was edited by hand; record a new one |

---

## Advanced

### Pattern data

A Pattern is `minecraft:paper` (drawn as a flow banner pattern) whose `custom_data` holds the
grid:

```snbt
{ra:{pattern:{slots:["minecraft:oak_planks","","","minecraft:oak_planks","","","","",""]}}}
```

- `slots` always has nine strings, in crafter slot order (`container.0` top left to
  `container.8` bottom right, row by row).
- Each is a namespaced item id, or `""` for an empty slot. No counts, no components.
- A blank Pattern has nine `""`.
- An item is a Pattern when its `custom_data` contains `ra.pattern`:
  `*[minecraft:custom_data~{ra:{pattern:{}}}]`.

The format is generic on purpose: other blocks read Patterns too (the Keypad will use one as a
code: two Patterns are equal when their `slots` lists are). Treat the ids as player data: an
edited item can hold anything, so check a value with `ra_lib:safe/check` (format ID) before
pasting it into a command, as the Crafter Controller does.

A custom block that takes Patterns adds itself to `Pattern.targets` (Kotlin,
`ra/modules/automation/pattern/Pattern.kt`): right-clicking it with a Pattern then runs its
function as the block's marker, the player tagged `ra.auto.pattern_user`.

### Crafter Controller data

| Where | What |
| --- | --- |
| `data.properties.enabled` | `1b` on, `0b` off |
| `data.properties.pattern` | the loaded Pattern's nine ids (read-only) |
| `data.data.item` | the Pattern itself, given back on eject or break |
| `data.data.match` | the ids after the format check, `minecraft:air` for an empty slot |
| `data.data.checked` | the list `match` was built from; when `pattern` no longer equals it, the Controller re-checks |
| `data.data.last` | the crafter's `Items` at the last look |
| `data.status.state`, `data.status.crafts` | what the Goggles show |

Only Controllers with a valid Pattern (tag `ra.auto.cc_armed`) are looked at, on a 10-tick
schedule that stops when none is loaded. A look stops at the first of: switched off, no
crafter, an empty crafter, a powered crafter, contents unchanged since the last look. The fire
is `setblock ~ ~ ~ minecraft:target[power=15]`, back to `power=0` two ticks later.
