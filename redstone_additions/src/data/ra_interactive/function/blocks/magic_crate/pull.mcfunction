# /ra_interactive:blocks/magic_crate/pull
# Internal: take one item entity into the crate.
# Context: as the item, at the crate's block.
#
# The whole stack crosses verbatim by appending the item entity's own Item
# compound to the container's Items list. Rebuilding it from an id and a count
# through a loot table would drop the components, and a named, enchanted or
# damaged item would arrive as a plain one.

execute unless block ~ ~ ~ barrel run return 0

# An item still on pickup delay was just thrown by a player who has not let go of
# it yet — a dropped stack teleporting back out of reach reads as the block
# eating your inventory.
execute if data entity @s {PickupDelay:32767s} run return 0

execute store result score #mh.slot ra.temp run function ra_lib:inventory/find_free_slot
execute if score #mh.slot ra.temp matches ..-1 run return 0

data modify storage ra:interactive mh.stack set from entity @s Item
execute store result storage ra:interactive mh.stack.Slot byte 1 run scoreboard players get #mh.slot ra.temp
data modify block ~ ~ ~ Items append from storage ra:interactive mh.stack

execute at @s run particle minecraft:portal ~ ~0.2 ~ 0.2 0.2 0.2 0.05 12
particle minecraft:enchant ~ ~1 ~ 0.3 0.3 0.3 0.4 8
playsound minecraft:entity.enderman.teleport block @a[distance=..12] ~ ~ ~ 0.2 1.8

scoreboard players add #mh.pulled ra.temp 1
kill @s
