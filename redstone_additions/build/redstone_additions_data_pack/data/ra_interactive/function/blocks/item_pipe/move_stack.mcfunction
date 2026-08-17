# /ra_interactive:blocks/item_pipe/move_stack {sx:"^ ^ ^-1"}
# Move the whole of the source container's first stack into the container at the
# current position, in one operation. Returns 1 on success.
#
# Cost of moving a full stack, before and after:
#   before  64 x (loot insert + data get + data modify)
#   after   1 free-slot lookup + 2 /item commands
#
# `loot insert` had to rebuild the item from an id, a count and a components blob
# every single time; /item replace copies the stack across as it stands.

execute unless block ~ ~ ~ #ra_lib:containers run return 0

# Never feed a vanilla dispenser; it would fire the contents.
execute if block ~ ~ ~ minecraft:dispenser unless entity @e[type=marker,tag=ra.custom_block,distance=..0.75,limit=1,sort=nearest] run return 0

$execute positioned $(sx) unless data block ~ ~ ~ Items[0] run return 0

# Items[0] is the first stack present, not slot 0, so read the slot it occupies.
$execute positioned $(sx) store result score #inv_src_slot ra.temp run data get block ~ ~ ~ Items[0].Slot

execute store result score #inv_dst_slot ra.temp run function ra_lib:inventory/find_free_slot
execute if score #inv_dst_slot ra.temp matches ..-1 run return 0

$data modify storage ra:inventory mv.src set value "$(sx)"
execute store result storage ra:inventory mv.src_slot int 1 run scoreboard players get #inv_src_slot ra.temp
execute store result storage ra:inventory mv.dst_slot int 1 run scoreboard players get #inv_dst_slot ra.temp
function ra_lib:inventory/move_slot with storage ra:inventory mv
data remove storage ra:inventory mv

return 1
