# /ra_wires:fluid/drain_potion
# Pour the network's potion over whoever is standing by the drain.
# Context: as the drain marker, at the drain position.
#
# The output side of the potion medium. A potion has no world block to place, so
# the thing a drain does with it is apply it -- one bottle's worth, 1000 mL, per
# operation, to every player within four blocks.
#
# DURATION FOLLOWS THE VOLUME
# The issue this implements asks for "a duration that matches the amount being
# drained and the potion duration field". So the effect's own duration is the
# duration of one full bottle, and a partial draw is a proportional share of it:
# 250 mL of a three-minute strength potion is forty-five seconds. Draining a
# network dry gives a shorter effect than draining a full one, which is what
# makes a tank farm of potion worth building.
#
# The level is the potion's own and is not scaled. Amplifier is a step, not a
# quantity -- half a bottle of strength II is still strength II, for less time.

execute if score @s ra.tr.net matches ..0 run return 0

function ra_lib:transport/net/read
execute unless data storage ra:transport cur.amounts.potion run return run data modify entity @s data.status.drain_state set value "nothing_to_pour"

execute store result score #pt.got ra.wires.tmp run function ra_lib:transport/net/take {amount:1000,medium:"potion"}
execute if score #pt.got ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value "nothing_to_pour"

# Nobody to pour it over is not a failure, but the potion has already left the
# network, so say so rather than silently binning it.
execute unless entity @a[distance=..4] run data modify entity @s data.status.drain_state set value "poured_away"
execute if entity @a[distance=..4] run data modify entity @s data.status.drain_state set value "applying"

data remove storage ra:wires apply
execute store result storage ra:wires apply.got int 1 run scoreboard players get #pt.got ra.wires.tmp
execute store result storage ra:wires apply.id int 1 run scoreboard players get @s ra.tr.net
function ra_wires:fluid/drain_potion_read with storage ra:wires apply

particle minecraft:effect ~ ~1 ~ 0.4 0.4 0.4 0.1 12 normal @a[distance=..16,scores={ra.u.par=1..}]
playsound minecraft:entity.generic.drink block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.1
