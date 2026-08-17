# /ra_wires:electric/try_receive_from_source
# Destination-side transfer logic. Context: as destination node marker.

execute unless entity @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] run return 0
execute if data entity @s data.properties{enabled:0b} run return 0
execute if data entity @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] data.properties{enabled:0b} run return 0

execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 200

execute store result score #src_eu ra.wires.tmp run data get entity @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] data.data.eu 1
execute if score #src_eu ra.wires.tmp matches ..0 run return 0

execute store result score #dst_eu ra.wires.tmp2 run data get entity @s data.data.eu 1
execute store result score #dst_cap ra.wires.tmp run data get entity @s data.data.capacity 1

scoreboard players operation #free ra.wires.tmp = #dst_cap ra.wires.tmp
scoreboard players operation #free ra.wires.tmp -= #dst_eu ra.wires.tmp2
execute if score #free ra.wires.tmp matches ..0 run return 0

# Downhill only, and only half the gap.
#
# Without this a node hands charge to whichever neighbour has room, including the
# one that just supplied it — and transfer_adjacent tries +X first, so charge
# bounced between the first two blocks of a run and never travelled further. A
# solar panel would fill the two wires next to it and leave the rest of the line at
# zero for ever.
#
# Half the gap rather than all of it, because moving the whole difference makes the
# pair swap places and oscillate; half converges, the way water finds its level.
scoreboard players operation #gap ra.wires.tmp2 = #src_eu ra.wires.tmp
scoreboard players operation #gap ra.wires.tmp2 -= #dst_eu ra.wires.tmp2
execute if score #gap ra.wires.tmp2 matches ..1 run return 0
scoreboard players set #two ra.wires.tmp2 2
scoreboard players operation #gap ra.wires.tmp2 /= #two ra.wires.tmp2

execute store result score #move ra.wires.tmp run data get entity @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] data.properties.transfer_rate 1
execute if score #gap ra.wires.tmp2 < #move ra.wires.tmp run scoreboard players operation #move ra.wires.tmp = #gap ra.wires.tmp2
execute if score #src_eu ra.wires.tmp < #move ra.wires.tmp run scoreboard players operation #move ra.wires.tmp = #src_eu ra.wires.tmp
execute if score #free ra.wires.tmp < #move ra.wires.tmp run scoreboard players operation #move ra.wires.tmp = #free ra.wires.tmp
execute if score #move ra.wires.tmp matches ..0 run return 0

scoreboard players operation #src_eu ra.wires.tmp -= #move ra.wires.tmp
scoreboard players operation #dst_eu ra.wires.tmp2 += #move ra.wires.tmp

execute store result entity @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] data.data.eu int 1 run scoreboard players get #src_eu ra.wires.tmp
execute store result entity @s data.data.eu int 1 run scoreboard players get #dst_eu ra.wires.tmp2

tag @e[type=marker,tag=ra.wires.eu_src,distance=..1.1,sort=nearest,limit=1] add ra.wires.did_move
