# /ra_interactive:blocks/message/tick
# Tick all message blocks

# Check for break detection (exclude new blocks, check for note_block)
execute as @e[tag=ra.custom_block.message_block,tag=!ra.new] at @s unless block ~ ~ ~ minecraft:note_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.message_block] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:note_block"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.message_block] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:note_block","minecraft:item_name":'Message Block',"minecraft:custom_data":{ra:{message_block:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.message_block"],Silent:1b,NoAI:1b,Invulnerable:1b}}},Tags:["ra.drop_temp"]}
execute as @e[tag=ra.broken,tag=ra.custom_block.message_block] at @s run playsound minecraft:block.wood.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.message_block] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.message_block] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.message_block] remove ra.broken

# Redstone detection
execute as @e[tag=ra.custom_block.message_block,tag=!ra.new] at @s run function ra_lib:redstone/detect

# Trigger on rising edge (just powered)
execute as @e[tag=ra.custom_block.message_block,tag=!ra.was_powered] if score @s ra.power matches 1.. at @s run function ra_interactive:blocks/message/send_message

# Track power state for edge detection
tag @e[tag=ra.custom_block.message_block,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.message_block,tag=!ra.powered] remove ra.was_powered

