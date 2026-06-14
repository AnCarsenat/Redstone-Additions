# /ra_interactive:blocks/message_block/send_message_block_macro
# Macro send path using explicit entity selector.
# Input: $(entity_selector)

$execute at @s as $(entity_selector) if entity @s[type=player] run tellraw @s [{text:"[Message] ",color:"yellow"},{nbt:"message_block",storage:"ra:temp",interpret:true}]
$execute at @s as $(entity_selector) if entity @s[type=player] run playsound minecraft:block.note_block.chime block @s ~ ~ ~ 0.5 1.2
