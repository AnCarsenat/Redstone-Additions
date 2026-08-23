# /ra_wires:fluid/drain_above_try {item,medium,volume,empty}
# Internal: one registry entry against the container above the drain.
# Context: as the drain marker, at the drain position.
#
# `container.*` rather than a slot, so it does not matter where in the barrel the
# bucket sits — the same reason the EU Generator stopped reading Items[0].
#
# The network is charged BEFORE the container is swapped, and the swap only
# happens if the offer was accepted in full. Emptying a bucket into a network
# with room for part of it would have to either destroy the remainder or hand
# back a partly full bucket, and there is no such item.

$execute unless items block ~ ~1 ~ container.* $(item) run return 0

function ra_lib:transport/net/read
$scoreboard players set #dr.need ra.wires.tmp $(volume)
scoreboard players operation #dr.free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #dr.free ra.wires.tmp -= #net_amount ra.tr.tmp
execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run data modify entity @s data.status.drain_state set value "network_full"

$execute store result score #dr.got ra.wires.tmp run function ra_lib:transport/net/offer {amount:$(volume),medium:"$(medium)"}
execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run data modify entity @s data.status.drain_state set value "wrong_medium"

# Paid for, so now take the full container and leave the empty behind.
#
# The removal is CHECKED. `if items` sees the whole container, ra_lib:inventory
# /remove reads the `Items` list, and the two do not agree on every block that
# accepts items -- and when the removal quietly failed, the empty was handed out
# anyway. That is one empty bucket per cycle, forever, out of a full one that
# never left the container, with the network being charged for water each time.
# Nothing is minted unless the full container actually came out.
$execute positioned ~ ~1 ~ store result score #dr.rm ra.wires.tmp run function ra_lib:inventory/remove {id:"$(item)",count:1}
$execute if score #dr.rm ra.wires.tmp matches ..0 run function ra_lib:transport/net/take {amount:$(volume),medium:"$(medium)"}
execute if score #dr.rm ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value "nothing_to_empty"

$execute positioned ~ ~1 ~ store result score #dr.put ra.wires.tmp run function ra_lib:inventory/insert {id:"$(empty)",count:1,components:{}}

# Removing one item does not guarantee a slot for the empty: the full stack may
# have been a partial one that merged, leaving the container still full of other
# things. Whatever will not fit is dropped rather than destroyed.
$execute if score #dr.put ra.wires.tmp matches ..0 run summon item ~ ~1.2 ~ {Item:{id:"$(empty)",count:1}}

scoreboard players set #dr.took ra.wires.tmp 1
data modify entity @s data.status.drain_state set value "emptied_container"
$function ra_wires:fluid/particles {medium:"$(medium)"}
playsound minecraft:item.bucket_empty block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.2
