# /ra_gates:blocks/rand/tick
# Tick all randomizers - outputs random signal on pulse

# Check for break detection
execute as @e[tag=ra.custom_block.rand] at @s unless block ~ ~ ~ bookshelf run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:bookshelf"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:bookshelf","minecraft:item_name":"Randomizer","minecraft:custom_data":{ra:{rand:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.rand"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run playsound minecraft:block.wood.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.rand] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.rand] remove ra.broken

# Process rand logic
execute as @e[tag=ra.custom_block.rand] at @s run function ra_gates:blocks/rand/process
