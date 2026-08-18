# /ra_wires:debug/generator
# Report every loaded EU Generator: /function ra_wires:debug/generator
#
# Written because "it says not active with coal in it" has several causes that
# look identical from outside the block, and reading the source has not settled
# which one it is.

tellraw @s [{text:"[RA] ",color:"gold"},{text:"EU Generators",color:"aqua"}]
execute unless entity @e[type=marker,tag=ra.custom_block.electric_generator] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s run function ra_wires:debug/generator_one
