# /ra_wires:common/update_model_local_and_neighbors
# Refresh current node and all immediate neighbors
# Context: as node marker, at node position

function ra_wires:common/update_model_single

execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
execute positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.node,distance=..0.99,limit=1] at @s run function ra_wires:common/update_model_single
