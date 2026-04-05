# /ra_lib:transport/update_connection_status
# Count adjacent transport nodes and store a compact status field
# Context: as node marker, at node position

scoreboard players set #conn ra.temp 0

execute positioned ~1 ~ ~ if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1
execute positioned ~-1 ~ ~ if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1
execute positioned ~ ~ ~1 if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1
execute positioned ~ ~ ~-1 if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1
execute positioned ~ ~1 ~ if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1
execute positioned ~ ~-1 ~ if entity @e[type=marker,tag=ra.wires.node,distance=..0.75,limit=1] run scoreboard players add #conn ra.temp 1

execute store result entity @s data.status.connections int 1 run scoreboard players get #conn ra.temp
execute if data entity @s data.properties.enabled run data modify entity @s data.status.enabled set from entity @s data.properties.enabled
