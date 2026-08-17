# /ra_infinite:debug/poppy
# Print what every Poppy Generator sees. Run it from chat: /function ra_infinite:debug/poppy
#
# Static reading of the code only gets so far — this reports the runtime values:
# where the marker is, which way it is turned, what the block in front is, and
# which of the ground rules match.

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Poppy Generator report",color:"aqua"}]
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] at @s run function ra_infinite:debug/poppy_one
