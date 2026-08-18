# /ra_interactive:blocks/breeder/tick
# Tick all breeders

# Check for break detection
execute as @e[type=marker,tag=ra.custom_block.breeder] at @s unless block ~ ~ ~ barrel run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] at @s run function ra_lib:skin/clear {id:"breeder"}
kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name": 'Breeder',"minecraft:custom_data":{ra:{breeder:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.breeder"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.breeder] remove ra.broken

# Check for powered dispensers - activate breeding
# Repaint the skin if it has gone missing.
execute as @e[type=marker,tag=ra.custom_block.breeder] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.breeder,distance=..0.4,limit=1] run function ra_interactive:blocks/breeder/refresh_display

# A barrel has no `triggered` state to read, so the redstone is read directly.
# ra_lib:redstone/any is the cheap reader: it stops at the first live side.
execute as @e[type=marker,tag=ra.custom_block.breeder] at @s if function ra_lib:redstone/any if data block ~ ~ ~ Items[0] run function ra_interactive:blocks/breeder/try_breed
