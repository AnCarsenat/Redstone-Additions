# Enchant Crafting

Gamble items on a plain enchanting table. Throw items on top of it: once a second the table eats
one and rolls — a hit gives you an upgrade, a miss destroys the item. This is how you get the
Cores for [Infinite Generators](infinite-generators.md) and the Infinite Jetpack Kit.

There is nothing to craft for that: any vanilla enchanting table works. Two crafted blocks
work with enchanted books, paid in liquid experience from a
[fluid network](transport-networks.md): the [Disenchanter](#disenchanter) moves an item's
enchantments onto a book.

## Using it

1. Place an enchanting table.
2. Drop (`Q`) the items you want to sacrifice on top of it.
3. Wait. Once a second the table takes **one** item and rolls.

| Outcome | You see | You get |
| --- | --- | --- |
| Success | Enchant and end-rod particles, an enchanting sound and a level-up chime | The upgrade, on the table |
| Failure | Lava and smoke particles, a hiss and a breaking sound | Nothing — the item is gone |

A stack of 64 takes 64 seconds and rolls 64 separate times. Pick the stack back up at any point
to stop. Items with no recipe are left alone on the table.

## Recipes

| Sacrifice | Result | Chance |
| --- | --- | --- |
| Stone | Mineral Core | 1% |
| Netherrack | Nether Core | 1% |
| Poppy | Poppy Core | 1% |
| Coal | Enchanted Coal | 1% |
| Iron Jetpack Kit | Infinite Iron Jetpack Kit | 1% |

Cores are half of an [Infinite Generator](infinite-generators.md); the other half is the crafted
Generator Casing.

## Disenchanter

![Disenchanter recipe](images/recipes/ra_enchanting/disenchanter.png){ width="220" }

Moves an item's enchantments onto a book.

1. Place the Disenchanter (it looks like crying obsidian).
2. Put a pipe, tank or [Liquid Drain](transport-networks.md#getting-fluid-in-and-out) holding
   **experience** against one of its sides or under it. A drain facing up takes a sneaking
   player's experience into the network.
3. Drop a **book** and an **enchanted item** on top.

Twice a second it checks what lies on it. With both there and the price in the network, it makes
an enchanted book holding every enchantment of the item, and the item stays on the block without
them. The book stack loses one book.

| | |
| --- | --- |
| Price | 1000 mL of experience (10 points) per enchantment moved, all at once. Server setting `disenchanter_xp` (0 to 100000; 0 makes it free and needs no network) |
| Curses | Curse of Binding and Curse of Vanishing stay on the item, as at a grindstone. An item with only curses is left alone |
| Repair cost | Reset, as at a grindstone, so the item is cheap to repair again |
| One at a time | One enchanted item per book: a stack of several enchanted items is left alone |

Items it cannot use (anything but a book or one enchanted item) are left where they are and no
longer looked at until someone picks them up. The Goggles show what it is doing (`needs a book`,
`needs liquid experience`, `disenchanted`, …) and the last price. Where the experience comes from:
the first network beside it that holds the whole price; nothing is taken from a network that
holds less.

---

## Advanced

### Adding your own recipes

Recipes come from the function tag `#ra_enchanting:recipes`, so an addon adds its own without
touching this module. A recipe list looks like this:

```mcfunction
# /my_addon:enchant_recipes
# An earlier recipe already matched: it wins, leave its result and chance alone.
execute if data storage ra:enchant result run return 0
execute if data storage ra:enchant input{id:"minecraft:stone"} run data modify storage ra:enchant result set value {id:"minecraft:diamond",count:1}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 5
execute if data storage ra:enchant result run return 1
```

| Storage | Meaning |
| --- | --- |
| `ra:enchant input` | the sacrificed item, components and all |
| `ra:enchant result` | the item to produce; leave it unset for "not mine" |
| `ra:enchant chance` | percent, `1..100`, default `5` |

The first recipe that matches wins: every list in the tag runs, so each must start with the
`if data … result run return 0` line. In the pack's own Kotlin, `sacrificeRecipe(...)`
(`ra/modules/enchanting/SacrificeRecipe.kt`) writes exactly these lines.

### The Disenchanter

Settings (Server Settings, Enchant Crafting page): `disenchanter_xp` and whether the block may be
placed. It is not a network node: like the Boiler, it reads the networks of the fluid nodes on
its four sides and below (`ra_enchanting:xp/pay`), so it never splits or joins one. It takes from
the first whose network holds the whole price of `experience`, with `ra_lib:transport/net/take`.
Only the vanilla curses (`minecraft:binding_curse`, `minecraft:vanishing_curse`) stay on the item;
a data pack's own curse moves to the book. Status for the Goggles is in `data.status.state` and
`data.status.price`. It has no properties.

### Cost

The enchanting blocks run every ten ticks from the same scan, and only a block with an untagged
item lying on it does any work: an empty one costs one entity test. Items they cannot use are
tagged `ra.ench.done`, like sacrifice products.

The table scan looks for item entities every five ticks. Items produced by a sacrifice are tagged
`ra.ench.done` and skipped from then on, so an upgrade landing back on the table is not
sacrificed again.
