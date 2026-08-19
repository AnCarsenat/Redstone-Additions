# /ra_wireless:tools/remote/change_channel
# Ask the player for a channel name (while sneaking).
#
# This used to print a [Set Channel] button that suggested
#   /function ra_wireless:tools/remote/set_channel {channel:"..."}
# which only works for a player who can run commands. On a survival server
# without cheats the remote could never be retuned from its default channel at
# all. It now goes through the shared book input, the same way the Data Handler
# asks for text, so it needs no permissions.

data modify storage ra:temp current_channel set from entity @s SelectedItem.components."minecraft:custom_data".ra.channel

tellraw @s ""
tellraw @s [{text:"══════ ",color:"dark_gray"},{text:"Set Channel",color:"red",bold:true},{text:" ══════",color:"dark_gray"}]
tellraw @s [{text:"Current: ",color:"gray"},{nbt:"current_channel",storage:"ra:temp",color:"yellow"}]

# Remember which hotbar slot the remote is in. The player is holding it right
# now, which is the only moment that is reliably true — once the Input Form book
# is in hand, SelectedItem is the book.
execute store result score @s ra.remote.slot run data get entity @s SelectedItemSlot

# Drop any half-finished request before starting a new one.
function ra_lib:input/cancel

scoreboard players set @s ra.remote.pending 1
function ra_lib:input/open_text

playsound minecraft:block.note_block.bell block @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.5 1.2
