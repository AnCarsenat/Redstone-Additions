# Enchant Crafting

Gamble items on a plain enchanting table. Throw items on top of it: once a second the table eats
one and rolls — a hit gives you an upgrade, a miss destroys the item. This is how you get the
Cores for [Infinite Generators](infinite-generators.md) and the Infinite Jetpack Kit.

There is nothing to craft: any vanilla enchanting table works.

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

### Cost

The table scan looks for item entities every five ticks. Items produced by a sacrifice are tagged
`ra.ench.done` and skipped from then on, so an upgrade landing back on the table is not
sacrificed again.
