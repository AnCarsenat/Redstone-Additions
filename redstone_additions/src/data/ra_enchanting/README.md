# ra_enchanting — sacrifice crafting

Drop items on a vanilla enchanting table. Once a second the table eats one item
off the stack and rolls: on a hit it spits out the upgraded item with enchant
particles and a level-up chime, on a miss the item is simply gone, with lava
particles and a hiss. The table itself is untouched vanilla — the mechanic is
entirely in the item scan.

## Registering recipes

`ra_enchanting:recipes/match` calls the function tag `#ra_enchanting:recipes`.
Add one function per addon there; it never needs to touch `ra_enchanting`
itself.

A recipe function reads and writes shared storage:

| Path | Direction | Meaning |
| --- | --- | --- |
| `storage ra:enchant input` | in | the sacrificed item compound (`{id,count,components}`) |
| `storage ra:enchant result` | out | the item compound to produce — absent means "not mine" |
| `storage ra:enchant chance` | out | success chance in percent, `1..100`; defaults to `5` |

The first list that writes `result` wins, so a recipe function must return early
once it has matched:

```mcfunction
# /my_addon:enchant_recipes
execute if data storage ra:enchant input{id:"minecraft:stone"} run data modify storage ra:enchant result set value {id:"minecraft:diamond",count:1}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 5
execute if data storage ra:enchant result run return 1
```

Match on `input{id:"…"}` for a plain item, or on the whole compound including
`components.\"minecraft:custom_data\"` to match one specific custom item.

## Cost

The scan is the only global `@e[type=item]` selector in the pack, and it runs
once every five ticks (`ra.ench.scan`). Produced items carry `ra.ench.done` and
are skipped forever after, so an upgrade landing back on the table it came from
is not re-sacrificed.
