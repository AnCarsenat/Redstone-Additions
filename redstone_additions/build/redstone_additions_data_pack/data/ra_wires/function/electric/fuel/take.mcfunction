# /ra_wires:electric/fuel/take {n,ticks,name}
# Internal: burn one item out of slot index n. Context: as the marker, at the block.
#
# Taking one off a stack, by index, on the real container. The count is read
# first so a stack of one is removed outright rather than left behind as a
# zero-count entry, which renders as a ghost item and cannot be picked up.
#
# The burn is only set after the item is gone, so a failed take cannot hand out
# free power.

$execute store result score #gen.cnt ra.wires.tmp run data get block ~ ~ ~ Items[$(n)].count
execute if score #gen.cnt ra.wires.tmp matches ..0 run return 0

$execute if score #gen.cnt ra.wires.tmp matches 1 run data remove block ~ ~ ~ Items[$(n)]
scoreboard players remove #gen.cnt ra.wires.tmp 1
$execute if score #gen.cnt ra.wires.tmp matches 1.. store result block ~ ~ ~ Items[$(n)].count int 1 run scoreboard players get #gen.cnt ra.wires.tmp

$data modify entity @s data.data.burn set value $(ticks)
$data modify entity @s data.status.fuel set value "$(name)"

playsound minecraft:block.furnace.fire_crackle block @a[distance=..12,scores={ra.u.snd=1..}] ~ ~ ~ 0.4 1.2
