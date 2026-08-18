# /ra_wires:blocks/electric_furnace/check_target {dx,dy,dz}
# Can the destination take one of what we are about to make?
# Context: as the marker, at the block. Writes #ef.can.
#
# Asked before anything is spent or consumed, so a full chest or a missing one
# simply stops the furnace instead of destroying an ingot.

scoreboard players set #ef.can ra.wires.tmp 0
data modify storage ra:temp inv_item set value {}
data modify storage ra:temp inv_item.id set from storage ra:wires ef.hit.result
data modify storage ra:temp inv_item.count set value 1

$execute positioned ~$(dx) ~$(dy) ~$(dz) unless block ~ ~ ~ #ra_lib:containers run return 0
$execute positioned ~$(dx) ~$(dy) ~$(dz) run function ra_lib:inventory/can_accept_one
scoreboard players operation #ef.can ra.wires.tmp = #inv_can_insert ra.temp
