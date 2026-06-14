# /ra_gates:blocks/shortener/tick
# Tick all shorteners - shortens signal to configurable pulse length

# Check for break detection
execute as @e[tag=ra.custom_block.shortener] at @s unless block ~ ~ ~ quartz_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:quartz_block"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:quartz_block","minecraft:item_name":"Shortener","minecraft:custom_data":{ra:{shortener:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.shortener"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.shortener] remove ra.broken

# Process shortener logic
execute as @e[tag=ra.custom_block.shortener] at @s run function ra_gates:blocks/shortener/process
