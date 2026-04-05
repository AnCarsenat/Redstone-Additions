# /ra_wires:liquid/try_receive_from_source
# Destination-side transfer logic. Context: as destination node marker.

execute unless entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] run return 0
execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties.enabled run data modify entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties.enabled set value 1b
execute unless data entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties.transfer_rate run data modify entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties.transfer_rate set value 50
execute unless data entity @s data.properties{enabled:1b} run return 0
execute unless data entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties{enabled:1b} run return 0
execute if entity @s[tag=ra.custom_block.liquid_pump] run return 0
execute if entity @s[tag=ra.custom_block.gas_pump] run return 0

execute unless data entity @s data.data.amount run data modify entity @s data.data.amount set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 200
execute unless data entity @s data.data.medium_id run data modify entity @s data.data.medium_id set value 0

execute store result score #src_amt ra.wires.tmp run data get entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.data.amount 1
execute store result score #src_mid ra.wires.tmp2 run data get entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.data.medium_id 1
execute if score #src_amt ra.wires.tmp matches ..0 run return 0
execute if score #src_mid ra.wires.tmp2 matches ..0 run return 0

execute store result score #dst_amt ra.wires.tmp run data get entity @s data.data.amount 1
execute store result score #dst_cap ra.wires.tmp2 run data get entity @s data.data.capacity 1
execute store result score #dst_mid ra.wires.tmp run data get entity @s data.data.medium_id 1

scoreboard players operation #cmp ra.wires.tmp2 = #src_mid ra.wires.tmp2
scoreboard players operation #cmp ra.wires.tmp2 -= #dst_mid ra.wires.tmp
execute if score #dst_amt ra.wires.tmp matches 1.. unless score #cmp ra.wires.tmp2 matches 0 run return 0

scoreboard players operation #free ra.wires.tmp2 = #dst_cap ra.wires.tmp2
scoreboard players operation #free ra.wires.tmp2 -= #dst_amt ra.wires.tmp
execute if score #free ra.wires.tmp2 matches ..0 run return 0

execute store result score #move ra.wires.tmp2 run data get entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.properties.transfer_rate 1
execute if score #src_amt ra.wires.tmp < #move ra.wires.tmp2 run scoreboard players operation #move ra.wires.tmp2 = #src_amt ra.wires.tmp
execute if score #free ra.wires.tmp2 < #move ra.wires.tmp2 run scoreboard players operation #move ra.wires.tmp2 = #free ra.wires.tmp2
execute if score #move ra.wires.tmp2 matches ..0 run return 0

execute if score #dst_amt ra.wires.tmp matches 0 run execute store result entity @s data.data.medium_id int 1 run scoreboard players get #src_mid ra.wires.tmp2

scoreboard players operation #src_amt ra.wires.tmp -= #move ra.wires.tmp2
scoreboard players operation #dst_amt ra.wires.tmp += #move ra.wires.tmp2

execute store result entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.data.amount int 1 run scoreboard players get #src_amt ra.wires.tmp
execute store result entity @s data.data.amount int 1 run scoreboard players get #dst_amt ra.wires.tmp
execute if score #src_amt ra.wires.tmp matches ..0 run data modify entity @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] data.data.medium_id set value 0

tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1,sort=nearest,limit=1] add ra.wires.did_move
