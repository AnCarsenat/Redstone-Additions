# /ra_wires:debug/electric
# Report every electric node: /function ra_wires:debug/electric
#
# Which grid each node is on and what that grid holds. Two nodes you believe are
# wired together but that report different grid ids are the whole diagnosis.

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Electric nodes",color:"aqua"}]
execute unless entity @e[type=marker,tag=ra.wires.electric_node] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:debug/electric_one
