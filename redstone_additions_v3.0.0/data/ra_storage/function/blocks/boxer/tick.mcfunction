# /ra_storage:blocks/boxer/tick
# Tick all boxer blocks

# Break detection
execute as @e[tag=ra.custom_block.boxer] at @s unless block ~ ~ ~ minecraft:dropper run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.boxer] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.boxer] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":"Boxer","minecraft:lore":[{text:"Compacts input1 container into output1 boxes",color:"gray",italic:false}],"minecraft:custom_data":{ra:{boxer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.boxer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.boxer] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.boxer] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.boxer] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.boxer] remove ra.broken

# Ensure IO defaults exist for macro processing
execute as @e[tag=ra.custom_block.boxer] unless data entity @s data.properties.input1 run data modify entity @s data.properties.input1 set value "^ ^ ^-1"
execute as @e[tag=ra.custom_block.boxer] unless data entity @s data.properties.output1 run data modify entity @s data.properties.output1 set value "^ ^ ^1"

# Migrate legacy up/down defaults to rear/front defaults.
execute as @e[tag=ra.custom_block.boxer,tag=!ra.storage.io_default_migrated] if data entity @s data.properties{input1:"^ ^-1 ^",output1:"^ ^1 ^"} run data merge entity @s {data:{properties:{input1:"^ ^ ^-1",output1:"^ ^ ^1"}}}
tag @e[tag=ra.custom_block.boxer,tag=!ra.storage.io_default_migrated] add ra.storage.io_default_migrated

# Redstone-powered processing
execute as @e[tag=ra.custom_block.boxer] at @s run function ra_lib:redstone/detect
execute as @e[tag=ra.custom_block.boxer,scores={ra.power=1..}] at @s run function ra_storage:blocks/boxer/process with entity @s data.properties
