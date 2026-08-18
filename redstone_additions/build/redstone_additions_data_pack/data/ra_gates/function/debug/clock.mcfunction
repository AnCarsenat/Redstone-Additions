# /ra_gates:debug/clock
# Report every loaded Clock: /function ra_gates:debug/clock
#
# Written because the Clock's symptom -- "it ignores its cooldown" -- has more
# than one cause that looks identical from across the room, and guessing between
# them from source alone has already been wrong twice.

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Clocks",color:"aqua"}]
execute unless entity @e[type=marker,tag=ra.custom_block.clock] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]
execute as @e[type=marker,tag=ra.custom_block.clock] at @s run function ra_gates:debug/clock_one
