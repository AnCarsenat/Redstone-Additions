# /ra:tools/data_handler/apply_pending
# Apply ready input values to the currently pending property edit.

execute unless entity @e[tag=ra.dh_target,limit=1] run scoreboard players set @s ra.dh.pending 0
execute unless entity @e[tag=ra.dh_target,limit=1] run return 0

execute store result score @s ra.temp run function ra_lib:input/poll
execute unless score @s ra.temp matches 2 run return 0

execute store result score @s ra.temp run function ra_lib:input/consume
execute unless score @s ra.temp matches 1 run return 0

# Numeric properties
execute if score @s ra.dh.pending matches 1 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.range set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 6 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.delay set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 7 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.cooldown set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 8 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.power set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 14 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.distance set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 15 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.extend set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 16 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.pulse set from storage ra:input consume.number
execute if score @s ra.dh.pending matches 17 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.chance set from storage ra:input consume.number

# Text properties
execute if score @s ra.dh.pending matches 2 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.message set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 3 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.tag set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 4 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.entity_selector set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 9 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.mode set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 10 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.target set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 12 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.gate_type set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 13 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.channel set from storage ra:input consume.text
execute if score @s ra.dh.pending matches 18 run data modify entity @e[tag=ra.dh_target,limit=1] data.properties.gate set from storage ra:input consume.text

scoreboard players set @s ra.dh.pending 0
tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Property updated.",color:"green"}]
function ra:tools/data_handler/refresh
