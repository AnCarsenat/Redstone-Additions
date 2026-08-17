# /ra_wires:debug/electric
# Report every electric node: /function ra_wires:debug/electric
#
# What the transfer step sees, in the transfer step's own terms — buffer, rate,
# whether the enabled flag is a byte at all, and how many electric neighbours the
# adjacency probe can actually reach from that block.

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Electric nodes",color:"aqua"}]
execute unless entity @e[type=marker,tag=ra.wires.electric_node] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:debug/electric_one
