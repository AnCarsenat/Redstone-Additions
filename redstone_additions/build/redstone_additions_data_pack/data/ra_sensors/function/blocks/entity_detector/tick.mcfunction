# /ra_sensors:blocks/entity_detector/tick
# Tick all entity detectors

# Check for break detection
execute as @e[tag=ra.custom_block.entity_detector] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.entity_detector] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:waxed_chiseled_copper"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.entity_detector] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:waxed_chiseled_copper","minecraft:item_name":'Entity Detector',"minecraft:custom_data":{ra:{entity_detector:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.entity_detector"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.entity_detector] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.entity_detector] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.entity_detector] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.entity_detector] remove ra.broken

# Check for entities in range
execute as @e[tag=ra.custom_block.entity_detector] at @s run function ra_sensors:blocks/entity_detector/detect
