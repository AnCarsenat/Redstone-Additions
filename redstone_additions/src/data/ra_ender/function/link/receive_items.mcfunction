# /ra_ender:link/receive_items
# Context: as the receiving vault marker, at its barrel. The pending move is in
# storage ra:ender move.
#
# The destination slot has to be empty before the stack is copied in: /item
# replace overwrites whatever is there, and an overwrite would destroy items.
# ra_lib:inventory/move_slot then clears the source, so the stack exists in
# exactly one place at every point.

execute unless block ~ ~ ~ barrel run return 0

function ra_lib:inventory/find_free_slot
execute if score #inv_slot ra.temp matches ..-1 run return 0

execute store result storage ra:ender move.dst_slot int 1 run scoreboard players get #inv_slot ra.temp
function ra_ender:link/move_stack with storage ra:ender move

# A delivery is not an insert: mark this end now, or the next cycle reads the
# arriving stack as something a player put in and pushes it back.
function ra_ender:blocks/item_vault/mark

scoreboard players set #ender.moved ra.temp 1
playsound minecraft:entity.enderman.teleport block @a[distance=..8] ~ ~ ~ 0.25 1.6
particle minecraft:portal ~ ~1 ~ 0.2 0.2 0.2 0.05 6
