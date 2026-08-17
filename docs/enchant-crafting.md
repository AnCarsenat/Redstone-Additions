# Enchant Crafting

!!! abstract "In short"
    Drop items on a plain enchanting table. Once a second it eats **one** of them and
    rolls: a hit hands back an upgrade, a miss destroys the item. Stone, netherrack
    and poppies each give a **1%** shot at a generator Core; an Iron Jetpack Kit has a
    **10%** shot at the infinite one. Anything with no recipe is left alone.

The `ra_enchanting` module turns a plain vanilla enchanting table into a gamble:
throw items on top of it and each one has a small chance of coming back as
something better. Everything else is lost.

- Namespace: `ra_enchanting`
- No item, no recipe, no block — the mechanic lives on the vanilla enchanting table
- Runtime architecture: [How It Works](how-it-works.md)

## Using it

1. Place an enchanting table.
2. Drop (`Q`) the items you want to sacrifice on top of it.
3. Wait. Once a second the table eats **one** item off the stack and rolls.

| Outcome | What you see | What you get |
| ------- | ------------ | ------------ |
| Success | Enchant and end-rod particles, `block.enchantment_table.use` and a level-up chime | The upgraded item, on the table |
| Failure | Lava and smoke particles, a hiss and an item-break snap | Nothing — the item is gone |

A stack of 64 therefore takes 64 seconds and rolls 64 separate times. Pick the
stack back up at any point to stop.

## Recipes

| Sacrifice | Result | Chance |
| --------- | ------ | ------ |
| `minecraft:stone` | Mineral Core | 1% |
| `minecraft:netherrack` | Nether Core | 1% |
| `minecraft:poppy` | Poppy Core | 1% |
| Iron Jetpack Kit | Infinite Iron Jetpack Kit | 10% |

Cores are half of an [Infinite Generator](infinite-generators.md) — the other
half is the crafted Generator Casing.

Items with no recipe are ignored — they sit on the table untouched.

## Adding your own

`ra_enchanting` owns the mechanic, not the recipe list. Recipes come from the
function tag `#ra_enchanting:recipes`, so an addon registers its own without
touching this module. The full contract is in
`redstone_additions/src/data/ra_enchanting/README.md`; the short version:

```mcfunction
# /my_addon:enchant_recipes
execute if data storage ra:enchant input{id:"minecraft:stone"} run data modify storage ra:enchant result set value {id:"minecraft:diamond",count:1}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 5
execute if data storage ra:enchant result run return 1
```

- `storage ra:enchant input` — the sacrificed item, components and all
- `storage ra:enchant result` — the item to produce; leaving it unset means "not mine"
- `storage ra:enchant chance` — percent, `1..100`, defaulting to `5`

## Cost

The scan is the only global `@e[type=item]` selector in the pack and it runs once
every five ticks rather than every tick. Items produced by a sacrifice are tagged
`ra.ench.done` and skipped from then on, so an upgrade that lands back on the
table it came from is not immediately sacrificed again.
