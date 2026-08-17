# /ra_wires:fluid/refresh_status_all
# Periodic status refresh for every fluid node.

scoreboard players set #status_timer ra.wires.tmp 0
execute as @e[type=marker,tag=ra.wires.fluid_node] run function ra_wires:fluid/refresh_status
