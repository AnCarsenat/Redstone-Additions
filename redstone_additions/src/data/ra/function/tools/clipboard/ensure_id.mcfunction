# /ra:tools/clipboard/ensure_id
# Give this player a clipboard slot number if they do not have one yet.
# Context: as the player.
#
# The payload cannot live on the item: /data refuses to modify a player, so there
# is no way to write it back into the stack you are holding. It cannot be one
# global slot either, or two players on a server would paste each other's
# settings. A small integer per player, assigned on first use, keys a slot in
# storage and never needs cleaning up.

execute unless score @s ra.clip.id matches 1.. run scoreboard players add #clip.next ra.temp 1
execute unless score @s ra.clip.id matches 1.. run scoreboard players operation @s ra.clip.id = #clip.next ra.temp
