# /ra_wires:fluid/drain_exp
# Draw a sneaking player's experience into the network.
# Context: as a vertical drain marker, at the drain position.
#
# One experience point is 100 mL, and that rate is used in both directions: what
# a player pours in is exactly what comes back out of a drain set to place. The
# conversion is deliberately a round number so a player can read a billboard in
# litres and know what they will get back.
#
# Whole points only. Experience is not divisible, so the network is charged for
# what can actually be taken off the player rather than for a fraction that would
# round away to nothing on the way out.

tag @a[distance=..2.5,predicate=ra:is_sneaking,sort=nearest,limit=1] add ra.wires.xp_giver
execute unless entity @a[tag=ra.wires.xp_giver,limit=1] run return 0

execute store result score #dr.pts ra.wires.tmp run xp query @a[tag=ra.wires.xp_giver,limit=1] points
execute if score #dr.pts ra.wires.tmp matches ..0 run return run function ra_wires:fluid/drain_exp_clear

# A network holding something else will not take experience.
function ra_lib:transport/net/read
execute if score #net_amount ra.tr.tmp matches 1.. unless data storage ra:transport cur{medium:"experience"} run data modify entity @s data.status.drain_state set value "wrong_medium"
execute if score #net_amount ra.tr.tmp matches 1.. unless data storage ra:transport cur{medium:"experience"} run return run function ra_wires:fluid/drain_exp_clear

# Ten points a cycle, or as much of that as the player has and the grid will hold.
scoreboard players set #dr.mul ra.wires.tmp 100
scoreboard players set #dr.cap ra.wires.tmp 10
execute if score #dr.pts ra.wires.tmp > #dr.cap ra.wires.tmp run scoreboard players operation #dr.pts ra.wires.tmp = #dr.cap ra.wires.tmp

scoreboard players operation #dr.free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #dr.free ra.wires.tmp -= #net_amount ra.tr.tmp
scoreboard players operation #dr.free ra.wires.tmp /= #dr.mul ra.wires.tmp
execute if score #dr.free ra.wires.tmp < #dr.pts ra.wires.tmp run scoreboard players operation #dr.pts ra.wires.tmp = #dr.free ra.wires.tmp

execute if score #dr.pts ra.wires.tmp matches ..0 run data modify entity @s data.status.drain_state set value "network_full"
execute if score #dr.pts ra.wires.tmp matches ..0 run return run function ra_wires:fluid/drain_exp_clear

# Charge the grid for exactly the points about to be taken, so neither side can
# end up owing the other a fraction of a point.
scoreboard players operation #dr.ml ra.wires.tmp = #dr.pts ra.wires.tmp
scoreboard players operation #dr.ml ra.wires.tmp *= #dr.mul ra.wires.tmp
execute store result storage ra:wires xp.amount int 1 run scoreboard players get #dr.ml ra.wires.tmp
function ra_wires:fluid/drain_exp_offer with storage ra:wires xp

execute store result storage ra:wires xp.points int 1 run scoreboard players get #dr.pts ra.wires.tmp
function ra_wires:fluid/drain_exp_take with storage ra:wires xp

data modify entity @s data.status.drain_state set value "absorbing_xp"
function ra_wires:fluid/particles {medium:"experience"}
playsound minecraft:entity.experience_orb.pickup block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4
function ra_wires:fluid/drain_exp_clear
