# /ra_wires:fluid/drain_item_try {item,medium,volume,empty}
# Internal: one registry entry against a nearby sneaking player's main hand.
# Context: as the drain marker, at the drain position.
#
# The candidate is marked rather than matched twice. `if items entity` is the
# version-stable way to read a held stack in this pack, but it needs the player
# as @s, and the network offer needs the drain as @s — so the player is tagged in
# their own context and picked back up by tag in ours.
#
# The network is charged BEFORE the item is swapped, and the swap only happens if
# the offer was accepted in full. Emptying a bucket into a network with room for
# part of it would have to either destroy the remainder or hand back a partly
# full bucket, and there is no such item.

$execute as @a[distance=..2.5,predicate=ra:is_sneaking,sort=nearest,limit=1] if items entity @s weapon.mainhand $(item) run tag @s add ra.wires.emptying
execute unless entity @a[tag=ra.wires.emptying,limit=1] run return 0

# Room for the whole container, or nothing happens.
function ra_lib:transport/net/read
$scoreboard players set #dr.need ra.wires.tmp $(volume)
scoreboard players operation #dr.free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #dr.free ra.wires.tmp -= #net_amount ra.tr.tmp
execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value "network_full"
execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @a[tag=ra.wires.emptying] remove ra.wires.emptying

$execute store result score #dr.got ra.wires.tmp run function ra_lib:transport/net/offer {amount:$(volume),medium:"$(medium)"}
execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value "wrong_medium"
execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @a[tag=ra.wires.emptying] remove ra.wires.emptying

# A potion is the medium `potion` by volume, but which potion it is has to be
# remembered before the bottle leaves the player's hand.
$execute if data storage ra:wires media.$(medium).effect run function ra_wires:fluid/potion_store

# Paid for, so now take the container and leave the empty behind.
$item replace entity @a[tag=ra.wires.emptying,limit=1] weapon.mainhand with $(empty)
tag @a[tag=ra.wires.emptying] remove ra.wires.emptying

scoreboard players set #dr.took ra.wires.tmp 1
data modify entity @s data.status.drain_state set value "emptied_container"
$function ra_wires:fluid/particles {medium:"$(medium)"}
playsound minecraft:item.bucket_empty block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.2
