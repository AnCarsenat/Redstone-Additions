# /ra_interactive:blocks/spitter/tick
# Tick all spitters

# Check for break detection
execute as @e[tag=ra.custom_block.spitter] at @s unless block ~ ~ ~ dropper run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[0]
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":"Spitter","minecraft:custom_data":{ra:{spitter:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.spitter"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.spitter] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.spitter] remove ra.broken

# Process each spitter - spit items if any in inventory
execute as @e[tag=ra.custom_block.spitter] at @s run function ra_interactive:blocks/spitter/process
