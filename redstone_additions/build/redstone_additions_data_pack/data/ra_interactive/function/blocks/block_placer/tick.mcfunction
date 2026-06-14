# /ra_interactive:blocks/block_placer/tick
# Tick all block placers

# Check for break detection
execute as @e[tag=ra.custom_block.block_placer] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[0]
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":"Block Placer","minecraft:custom_data":{ra:{block_placer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.block_placer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.block_placer] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.block_placer] remove ra.broken

# Check for powered dispensers - instant activation (no cooldown)
execute as @e[tag=ra.custom_block.block_placer] at @s if block ~ ~ ~ dispenser[triggered=true] run execute if block ^ ^ ^1 air if data block ~ ~ ~ Items[0] run data modify storage ra:temp place_item set from block ~ ~ ~ Items[0]
execute as @e[tag=ra.custom_block.block_placer] at @s if block ~ ~ ~ dispenser[triggered=true] run execute if block ^ ^ ^1 air if data block ~ ~ ~ Items[0] positioned ^ ^ ^1 run function ra_interactive:blocks/block_placer/place_block with storage ra:temp place_item
execute as @e[tag=ra.custom_block.block_placer] at @s if block ~ ~ ~ dispenser[triggered=true] run execute if block ^ ^ ^1 air if data block ~ ~ ~ Items[0] run playsound minecraft:block.stone.place block @a[distance=..16] ^ ^ ^1 1 0.8
execute as @e[tag=ra.custom_block.block_placer] at @s if block ~ ~ ~ dispenser[triggered=true] run execute if block ^ ^ ^1 air if data block ~ ~ ~ Items[0] run particle minecraft:cloud ^ ^ ^1 0.2 0.2 0.2 0.02 5
