# /ra_sensors:blocks/tag_remover/tick
# Tick all tag removers

# Check for break detection
execute as @e[tag=ra.custom_block.tag_remover] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_remover] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:red_glazed_terracotta"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_remover] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:red_glazed_terracotta","minecraft:item_name":'Tag Remover',"minecraft:custom_data":{ra:{tag_remover:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.tag_remover"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_remover] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_remover] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.tag_remover] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.tag_remover] remove ra.broken

# Redstone detection
execute as @e[tag=ra.custom_block.tag_remover] at @s run function ra_lib:redstone/detect

# Trigger on rising edge (just powered)
execute as @e[tag=ra.custom_block.tag_remover,tag=ra.powered,tag=!ra.was_powered] at @s run data modify storage ra:temp tag set from entity @s data.properties.tag
execute as @e[tag=ra.custom_block.tag_remover,tag=ra.powered,tag=!ra.was_powered] at @s run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector
execute as @e[tag=ra.custom_block.tag_remover,tag=ra.powered,tag=!ra.was_powered] at @s run execute if data entity @s data.properties.tag run execute unless data entity @s {data:{properties:{tag:""}}} run function ra_sensors:blocks/tag_remover/remove_tag_macro with storage ra:temp
execute as @e[tag=ra.custom_block.tag_remover,tag=ra.powered,tag=!ra.was_powered] at @s run execute if data entity @s {data:{properties:{tag:""}}} run function ra_sensors:blocks/tag_remover/clear_all_tags_macro with storage ra:temp
execute as @e[tag=ra.custom_block.tag_remover,tag=ra.powered,tag=!ra.was_powered] at @s run execute unless data entity @s data.properties.tag run function ra_sensors:blocks/tag_remover/clear_all_tags_macro with storage ra:temp

# Track power state for edge detection
tag @e[tag=ra.custom_block.tag_remover,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.tag_remover,tag=!ra.powered] remove ra.was_powered
