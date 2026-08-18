# /ra_wires:fluid/drain_place_exp
# Give experience back to the world as orbs.
# Context: as a drain marker in "place" mode, at the drain position.
#
# The same 100 mL a point that drain_exp charges, so a network filled by standing
# on one drain empties at exactly the rate it filled — ten points a cycle. What
# went in comes back out; the network is not a lossy converter.

function ra_lib:transport/net/read

scoreboard players set #dr.mul ra.wires.tmp 100
scoreboard players set #dr.pts ra.wires.tmp 10

# Whole points only, and never more than the network holds.
scoreboard players operation #dr.have ra.wires.tmp = #net_amount ra.tr.tmp
scoreboard players operation #dr.have ra.wires.tmp /= #dr.mul ra.wires.tmp
execute if score #dr.have ra.wires.tmp < #dr.pts ra.wires.tmp run scoreboard players operation #dr.pts ra.wires.tmp = #dr.have ra.wires.tmp
execute if score #dr.pts ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value "not_enough"

scoreboard players operation #dr.ml ra.wires.tmp = #dr.pts ra.wires.tmp
scoreboard players operation #dr.ml ra.wires.tmp *= #dr.mul ra.wires.tmp

execute store result storage ra:wires xp.amount int 1 run scoreboard players get #dr.ml ra.wires.tmp
function ra_wires:fluid/drain_exp_pull with storage ra:wires xp

execute store result storage ra:wires xp.points int 1 run scoreboard players get #dr.pts ra.wires.tmp
function ra_wires:fluid/drain_place_orb with storage ra:wires xp

data modify entity @s data.status.drain_state set value "placing"
function ra_wires:fluid/particles {medium:"experience"}
