# /ra_wireless:blocks/emitter/tick
# Tick all emitters

# Check for break detection (using end_stone_bricks as emitter block)
execute as @e[tag=ra.custom_block.emitter] at @s unless block ~ ~ ~ end_stone_bricks run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.emitter] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:end_stone_bricks"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.emitter] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:end_stone_bricks","minecraft:item_name":"Wireless Emitter","minecraft:custom_data":{ra:{emitter:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.emitter"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.emitter] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.emitter] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.emitter] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.emitter] remove ra.broken

# Process emitter logic
execute as @e[tag=ra.custom_block.emitter] at @s run function ra_wireless:blocks/emitter/process
