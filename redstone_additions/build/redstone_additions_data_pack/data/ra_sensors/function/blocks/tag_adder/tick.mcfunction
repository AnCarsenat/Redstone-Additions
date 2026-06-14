# /ra_sensors:blocks/tag_adder/tick
# Tick all tag adders

# Check for break detection
execute as @e[tag=ra.custom_block.tag_adder] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_adder] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:green_glazed_terracotta"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_adder] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:green_glazed_terracotta","minecraft:item_name":'Tag Adder',"minecraft:custom_data":{ra:{tag_adder:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.tag_adder"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_adder] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_adder] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_adder] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.tag_adder] remove ra.broken

# Redstone detection
execute as @e[tag=ra.custom_block.tag_adder] at @s run function ra_lib:redstone/detect

# Trigger on rising edge (just powered)
execute as @e[tag=ra.custom_block.tag_adder,tag=ra.powered,tag=!ra.was_powered] at @s run data modify storage ra:temp tag set from entity @s data.properties.tag
execute as @e[tag=ra.custom_block.tag_adder,tag=ra.powered,tag=!ra.was_powered] at @s run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector
execute as @e[tag=ra.custom_block.tag_adder,tag=ra.powered,tag=!ra.was_powered] at @s run function ra_sensors:blocks/tag_adder/add_tag_macro with storage ra:temp

# Track power state for edge detection
tag @e[tag=ra.custom_block.tag_adder,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.tag_adder,tag=!ra.powered] remove ra.was_powered
