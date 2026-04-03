# /ra_gates:blocks/extender/on_break
# Handle Extender break

# Kill vanilla quartz_block drop
kill @e[type=item,nbt={Item:{id:"minecraft:quartz_block"}},distance=..2,limit=1,sort=nearest]

# Revert any redstone blocks back to iron blocks
fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Drop the Extender item
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:quartz_block","minecraft:item_name":"Extender","minecraft:custom_data":{ra:{extender:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.extender"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5

kill @s
