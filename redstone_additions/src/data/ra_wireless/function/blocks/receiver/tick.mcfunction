# /ra_wireless:blocks/receiver/tick
# Tick all receivers

# Check for break detection (using purpur_block as receiver block)
execute as @e[tag=ra.custom_block.receiver] at @s unless block ~ ~ ~ purpur_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:purpur_block"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:purpur_block","minecraft:item_name":"Wireless Receiver","minecraft:custom_data":{ra:{receiver:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.receiver"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.receiver] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.receiver] remove ra.broken

# Process receiver logic
execute as @e[tag=ra.custom_block.receiver] at @s run function ra_wireless:blocks/receiver/process
