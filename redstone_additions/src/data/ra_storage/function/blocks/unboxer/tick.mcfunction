# /ra_storage:blocks/unboxer/tick
# Tick all unboxer blocks

# Break detection
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s unless block ~ ~ ~ minecraft:dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":"Unboxer","minecraft:lore":[{text:"Unboxes boxes from the input side into the output side",color:"gray",italic:false}],"minecraft:custom_data":{ra:{unboxer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.unboxer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] remove ra.broken

# Ensure IO defaults exist for macro processing
execute as @e[type=marker,tag=ra.custom_block.unboxer] unless data entity @s data.properties.input1 run data modify entity @s data.properties.input1 set value "^ ^ ^-1"
execute as @e[type=marker,tag=ra.custom_block.unboxer] unless data entity @s data.properties.output1 run data modify entity @s data.properties.output1 set value "^ ^ ^1"

# Migrate legacy up/down defaults to rear/front defaults.
execute as @e[type=marker,tag=ra.custom_block.unboxer,tag=!ra.storage.io_default_migrated] if data entity @s data.properties{input1:"^ ^-1 ^",output1:"^ ^1 ^"} run data merge entity @s {data:{properties:{input1:"^ ^ ^-1",output1:"^ ^ ^1"}}}
tag @e[type=marker,tag=ra.custom_block.unboxer,tag=!ra.storage.io_default_migrated] add ra.storage.io_default_migrated

# Processing
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s run function ra_lib:redstone/detect
# Redstone LOCKS this block, the way it locks a hopper; it does not start it.
# The block is a vanilla dispenser, and a vanilla dispenser fires its own contents on
# a rising redstone edge. Requiring power to run therefore meant that operating
# the block also shot whatever was inside it out into the world — which is how an
# unboxed box ended up on the floor. Running unpowered removes the trigger
# entirely, and leaves redstone as a way to pause the machine.
execute as @e[type=marker,tag=ra.custom_block.unboxer,scores={ra.power=0}] at @s run function ra_storage:blocks/unboxer/process with entity @s data.properties
