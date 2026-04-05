# /ra_wires:liquid/transfer_adjacent
# Attempt transfer in deterministic neighbor order

execute if entity @s[tag=!ra.wires.did_move] positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
execute if entity @s[tag=!ra.wires.did_move] positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
execute if entity @s[tag=!ra.wires.did_move] positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
execute if entity @s[tag=!ra.wires.did_move] positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
execute if entity @s[tag=!ra.wires.did_move] positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
execute if entity @s[tag=!ra.wires.did_move] positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.99,limit=1,sort=nearest] at @s run function ra_wires:liquid/try_receive_from_source
