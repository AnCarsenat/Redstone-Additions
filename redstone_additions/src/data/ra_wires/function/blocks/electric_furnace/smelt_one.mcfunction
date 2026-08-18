# /ra_wires:blocks/electric_furnace/smelt_one
# Consume one item from the found input slot and push its result out.
# Context: as the marker, at the block.
#
# #ef.took is cleared HERE, not only inside take_input. A macro function whose
# arguments are incomplete fails without running a single one of its lines, so a
# flag it was supposed to reset can survive from the last call -- and a stale
# "yes I took it" is exactly how this duplicated items. Clearing it in the caller
# means a take that does not run reads as a take that did not happen.

scoreboard players set #ef.took ra.wires.tmp 0
function ra_wires:blocks/electric_furnace/take_input with storage ra:wires ef.hit
execute if score #ef.took ra.wires.tmp matches ..0 run return 0

function ra_wires:blocks/electric_furnace/deliver with storage ra:wires ef.out

data modify entity @s data.status.state set value "Smelting"
function ra_wires:blocks/electric_furnace/running
