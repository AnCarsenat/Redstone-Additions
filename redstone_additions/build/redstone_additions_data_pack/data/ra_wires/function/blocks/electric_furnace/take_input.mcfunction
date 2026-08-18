# /ra_wires:blocks/electric_furnace/take_input {slot,result}
# Internal: take one item off the input stack in $(slot). Context: at the block.
#
# By Slot rather than by list index, so it does not matter how the barrel is
# arranged. The count is read first so a stack of one is removed outright instead
# of left as a zero-count entry, which renders as a ghost item.

scoreboard players set #ef.took ra.wires.tmp 0

$execute store result score #ef.cnt ra.wires.tmp run data get block ~ ~ ~ Items[{Slot:$(slot)b}].count
execute if score #ef.cnt ra.wires.tmp matches ..0 run return 0

$execute if score #ef.cnt ra.wires.tmp matches 1 run data remove block ~ ~ ~ Items[{Slot:$(slot)b}]
scoreboard players remove #ef.cnt ra.wires.tmp 1
$execute if score #ef.cnt ra.wires.tmp matches 1.. store result block ~ ~ ~ Items[{Slot:$(slot)b}].count int 1 run scoreboard players get #ef.cnt ra.wires.tmp

scoreboard players set #ef.took ra.wires.tmp 1
