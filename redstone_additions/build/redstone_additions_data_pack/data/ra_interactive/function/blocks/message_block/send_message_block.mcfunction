# /ra_interactive:blocks/message_block/send_message_block
# Send the message_block to selected players. As armor stand, at position.

# Copy message_block to storage
data modify storage ra:temp message_block set from entity @s data.properties.message_block

# Preferred targeting model: explicit entity selector string.
execute if data entity @s data.properties.entity_selector run data modify storage ra:temp entity_selector set from entity @s data.properties.entity_selector
execute if data entity @s data.properties.entity_selector run function ra_interactive:blocks/message_block/send_message_block_macro with storage ra:temp
execute if data entity @s data.properties.entity_selector run return 1

# Legacy fallback for old blocks that still use range.
execute store result score @s ra.temp run data get entity @s data.properties.range
execute if score @s ra.temp matches ..0 run scoreboard players set @s ra.temp 16

# Send tellraw to players within range (use execute if to check range brackets)
# Use interpret:true to parse JSON text components in the message_block
execute if score @s ra.temp matches ..8 run tellraw @a[distance=..8] [{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]
execute if score @s ra.temp matches 9..16 run tellraw @a[distance=..16] [{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]
execute if score @s ra.temp matches 17..32 run tellraw @a[distance=..32] [{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]
execute if score @s ra.temp matches 33.. run tellraw @a[distance=..64] [{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]

execute if score @s ra.temp matches ..16 run playsound minecraft:block.note_block.chime block @a[distance=..16] ~ ~ ~ 0.5 1.2
execute if score @s ra.temp matches 17..32 run playsound minecraft:block.note_block.chime block @a[distance=..32] ~ ~ ~ 0.5 1.2
execute if score @s ra.temp matches 33.. run playsound minecraft:block.note_block.chime block @a[distance=..64] ~ ~ ~ 0.5 1.2
