# /ra_wires:fluid/drain_loose_try {item,medium,volume,empty}
# Internal: one registry entry against the item entities above the drain.
# Context: as the drain marker, at the drain position.
#
# The candidate is tagged rather than matched twice: the count has to be read and
# written on the same entity the offer was made for, and re-running the selector
# between those two steps could land on a different stack.
#
# Only the block directly above is searched. A drain should take what has been
# dropped ON it, not vacuum the floor around it — that is the Magic Crate's job.

$execute positioned ~ ~1 ~ as @e[type=item,distance=..1.2,nbt={Item:{id:"$(item)"}},sort=nearest,limit=1] run tag @s add ra.wires.draining_item
execute unless entity @e[type=item,tag=ra.wires.draining_item,limit=1] run return 0

function ra_lib:transport/net/read
$scoreboard players set #dr.need ra.wires.tmp $(volume)
scoreboard players operation #dr.free ra.wires.tmp = #net_capacity ra.tr.tmp
scoreboard players operation #dr.free ra.wires.tmp -= #net_amount ra.tr.tmp
execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value "network_full"
execute if score #dr.free ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item

$execute store result score #dr.got ra.wires.tmp run function ra_lib:transport/net/offer {amount:$(volume),medium:"$(medium)"}
execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run data modify entity @s data.status.drain_state set value "wrong_medium"
execute if score #dr.got ra.wires.tmp < #dr.need ra.wires.tmp run return run tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item

# Paid for. One item off the stack — buckets and potions never stack, but
# experience bottles do, and killing the whole stack for one bottle's worth of
# experience would eat the other sixty-three.
execute store result score #dr.cnt ra.wires.tmp run data get entity @e[type=item,tag=ra.wires.draining_item,limit=1] Item.count
scoreboard players remove #dr.cnt ra.wires.tmp 1
execute if score #dr.cnt ra.wires.tmp matches ..0 run kill @e[type=item,tag=ra.wires.draining_item]
execute if score #dr.cnt ra.wires.tmp matches 1.. store result entity @e[type=item,tag=ra.wires.draining_item,limit=1] Item.count int 1 run scoreboard players get #dr.cnt ra.wires.tmp
tag @e[type=item,tag=ra.wires.draining_item] remove ra.wires.draining_item

$summon item ~ ~1.2 ~ {Item:{id:"$(empty)",count:1}}

scoreboard players set #dr.took ra.wires.tmp 1
data modify entity @s data.status.drain_state set value "emptied_container"
$function ra_wires:fluid/particles {medium:"$(medium)"}
playsound minecraft:item.bucket_empty block @a[distance=..8,scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.2
