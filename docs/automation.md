# Automation

Blocks and items that run vanilla machines for you. The **Crafter Controller** fires a vanilla
crafter on its own once the crafter holds the ingredients its **Pattern** asks for. The
**Uncrafter** takes items back apart into their ingredients for EU, and the **Experience
Absorber** collects experience orbs into a fluid network as liquid experience.

## Getting them

| Item | Looks like | Recipe |
| --- | --- | --- |
| Crafter Controller | target block | ![Crafter Controller recipe](images/recipes/ra_automation/crafter_controller.png){ width="220" } |
| Pattern | flow banner pattern | ![Pattern recipe](images/recipes/ra_automation/pattern.png){ width="220" } |
| Uncrafter | chiseled tuff bricks | ![Uncrafter recipe](images/recipes/ra_automation/uncrafter.png){ width="220" } |
| Experience Absorber | verdant froglight | ![Experience Absorber recipe](images/recipes/ra_automation/experience_absorber.png){ width="220" } |

Operators: `/function ra_automation:items/give_all` (a Crafter Controller, a blank Pattern, an
Uncrafter and an Experience Absorber),
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

Patterns stack to 64: identical Patterns (the same grid) stack together, blank ones with blank
ones. The recipe makes three. Holding a stack, recording or clearing acts on one Pattern only:
one is taken from the stack and the new one goes into your inventory (or lands at your feet when
it is full), so the rest of the stack keeps its grid. A Crafter Controller takes one Pattern from
a held stack.

## Crafter Controller

1. Place a vanilla **crafter** and fill its grid once by hand with the recipe you want.
2. Record it with a Pattern (sneak + right-click the crafter).
3. Place the **Crafter Controller** right against the crafter: under it, beside it or on top.
4. Right-click the Controller with the Pattern. The Controller keeps it (one from a stack); if it
   already held one, that one comes back to you.
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

## Uncrafter

Takes items back apart into the ingredients of their vanilla crafting recipe: an iron block
into nine iron ingots, a hopper into five iron ingots and a chest, three iron doors into six
iron ingots. Each item taken apart costs a lot of EU.

1. Put a container **on top** of the Uncrafter (a hopper, chest or barrel): that is its input.
2. Put a container **under** it: that is where the ingredients go.
3. Touch one of its four sides with an EU wire or a Battery on a grid holding enough EU.

Once a second it looks at the input, slot by slot, and acts on the first stack it can:

| The stack is | What happens |
| --- | --- |
| an item it knows, with at least as many as the recipe makes | that many are taken apart; the ingredients go into the container below, for *eu_per_uncraft* EU (default 2000) |
| an item it knows, with fewer than the recipe makes (two of the three iron doors) | it waits for more |
| damaged, enchanted, renamed, dyed or changed in any other way | passed to the container below unchanged, for free |
| an item it does not know | passed to the container below unchanged, for free |

- It never gives back more than went in: only the exact vanilla recipe, and only recipes where
  taking apart cannot beat crafting (no stairs, which a stonecutter makes more cheaply; no
  recipes taking "any planks"; nothing that hands back buckets or bottles).
- It checks everything before it moves anything: the container below needs one empty slot per
  ingredient, and the grid must hold the whole price. If either is missing, nothing is taken and
  nothing is paid.
- The EU comes from the first wire or battery beside it whose grid holds the whole price. A grid
  with no Battery holds almost nothing, so put a Battery on it.
- A redstone signal stops it.

What it takes apart:

| Group | Items |
| --- | --- |
| Storage blocks | iron, gold, diamond, emerald, lapis, redstone, coal, copper, netherite, raw iron, raw gold, raw copper, quartz, bone, slime, hay, dried kelp, snow, glowstone, clay, bricks; and the netherite ingot (four scrap, four gold) |
| Tools and armour | iron, golden and diamond swords, pickaxes, axes, shovels, hoes, helmets, chestplates, leggings and boots; leather armour; turtle shell; bow; crossbow; shears; compass; clock; spyglass |
| Redstone and machines | hopper, observer, dispenser, dropper, repeater, comparator, crafter, target, weighted pressure plates, iron door (3), iron trapdoor, iron bars (16), minecart, bucket, cauldron, anvil, lantern |
| Late game | enchanting table, ender chest, eye of ender, beacon, conduit, lodestone, respawn anchor |

The number in brackets is how many of the item one recipe makes, and so how many it waits for.

### Wrench and tools

| Tool | Does |
| --- | --- |
| Goggles | State (waiting for items, uncrafting, passing items on, waiting for a full recipe, not enough EU, output full, no container above / below, stopped by redstone), the number of items taken apart and the EU per item |
| Data Handler | `eu_per_uncraft` (admins only) |

The default *eu_per_uncraft* for new Uncrafters is on the Automation page of the server
settings.

## Experience Absorber

Collects the experience orbs around it and sends them, as liquid experience, into a fluid network
touching it. One point is 100 mL, the same rate the Fluid Drain uses and the Disenchanter, Book
Upgrader and Book Printer charge in, so the experience it collects pays for those directly.

1. Place it where the orbs fall: beside a mob farm's kill spot, a furnace array, a breeding pen.
2. Touch one of its faces with a pipe, tank or drain.

- Once a second it takes the orbs within *radius* blocks (default 4, up to 8; admins up to 16),
  nearest first.
- It only takes what the network has room for. An orb bigger than the room left is taken in part
  and the rest stays on the ground.
- A network that holds something else and no experience is left alone, as with the Fluid Drain.
  It uses the first network beside it with room.
- Players near the orbs still pick them up first.

### Wrench and tools

| Tool | Does |
| --- | --- |
| Wrench, Shift+RMB | Cycles the radius: 2, 4, 6, 8 |
| Goggles | State (absorbing, no experience around, no room in a network beside it), the radius and the points absorbed so far |
| Data Handler | `radius` (1 to 8; admins up to 16) |

## Troubleshooting

| Goggles say | Check |
| --- | --- |
| no pattern | Right-click it with a recorded Pattern |
| no crafter next to it | The crafter must touch the Controller face to face |
| waiting for the ingredients | Some slot holds a different item, or holds something where the Pattern has an empty slot |
| crafter powered | Something else keeps the crafter powered; it needs to be unpowered to be fired |
| crafting, but nothing comes out | The ingredients are not a real recipe, or the crafter's output is blocked |
| pattern unreadable | The Pattern was edited by hand; record a new one |
| Uncrafter: not enough EU | No wire or battery beside it, or its grid holds less than the price: add a Battery |
| Uncrafter: output full | The container below needs one empty slot per ingredient |
| Uncrafter: waiting for a full recipe | The stack is smaller than what the recipe makes (3 iron doors, 16 iron bars) |
| Absorber: no room in a network beside it | No pipe, tank or drain touching it, the network is full, or it holds another fluid |

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
- Patterns stack (`max_stack_size` 64). The item carries nothing per-item: two Patterns are the
  same stack exactly when their `slots` are equal (the lore is built from `slots`). The use is
  detected through an endless `consumable` (a `using_item` advancement), which works on a stack
  as on a single item.

The format is generic on purpose, so other blocks and add-ons can read Patterns too. Treat the ids as player data: an
edited item can hold anything, so check a value with `ra_lib:safe/check` (format ID) before
pasting it into a command, as the Crafter Controller does.

A custom block that takes Patterns adds itself to `Pattern.targets` (Kotlin,
`ra/modules/automation/pattern/Pattern.kt`): right-clicking it with a Pattern then runs its
function as the block's marker, the player tagged `ra.auto.pattern_user`. The main hand may hold
a stack: a block that keeps the Pattern takes one (`Pattern.TAKE_ONE`, a `set_count` -1
modifier), a block that only compares it leaves the stack alone.

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

### Uncrafter data

| Where | What |
| --- | --- |
| `data.properties.eu_per_uncraft` | EU per item taken apart (admin only) |
| `data.status.state`, `data.status.uncrafted` | what the Goggles show |
| `storage ra_automation:uncraft map` | the reverse recipes, written at load: `{"minecraft:iron_door":{n:3,out:[{id:"minecraft:iron_ingot",count:6}]}}` |

The table is built in Kotlin (`ra/modules/automation/uncrafter/UncraftTable.kt`) from a curated
list checked against the vanilla recipe files; the comment there says what is left out and why.
An add-on can add a row to the storage after load, in the same shape; `n` is how many one recipe
makes, and each ingredient's count must fit in one stack.

### Experience Absorber data

| Where | What |
| --- | --- |
| `data.properties.radius` | reach in blocks, 1 to 8 (16 for an admin) |
| `data.status.state`, `data.status.absorbed` | what the Goggles show; `absorbed` is in points |

Both blocks run on one 20-tick schedule, started at load (markers tagged `ra.auto.clocked`);
nothing of theirs runs per tick but the break checks, and with neither loaded a run is two
selectors a second. Neither is a network node: like the Boiler, they reach into the network of a
node touching them, so placing or breaking one never splits or joins a network. An Absorber with
no orb within 16 blocks costs one selector a second.
