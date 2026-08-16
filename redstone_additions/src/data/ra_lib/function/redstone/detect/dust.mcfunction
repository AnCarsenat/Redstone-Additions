# /ra_lib:redstone/detect/dust
# Internal: Detect redstone dust connected toward this block with exact power (1-15).
#
# Each direction is gated on a single connection test before the 15 power
# comparisons run, so a block with no dust around it costs 8 commands instead
# of the 128 an unconditional sweep would need. `side` and `up` connections
# resolve to the same power score, so both share one resolver per direction.

# North input (dust at z-1 connected south)
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side] unless block ~ ~ ~-1 minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/north
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up] unless block ~ ~ ~-1 minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/north

# South input (dust at z+1 connected north)
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side] unless block ~ ~ ~1 minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/south
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up] unless block ~ ~ ~1 minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/south

# West input (dust at x-1 connected east)
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side] unless block ~-1 ~ ~ minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/west
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up] unless block ~-1 ~ ~ minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/west

# East input (dust at x+1 connected west)
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side] unless block ~1 ~ ~ minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/east
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up] unless block ~1 ~ ~ minecraft:redstone_wire[power=0] run function ra_lib:redstone/detect/dust/east
