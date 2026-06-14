# /ra_lib:redstone/detect/dust
# Internal: Detect redstone dust connected toward this block with exact power (1-15).

# North input (dust at z-1 connected south)
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side] unless block ~ ~ ~-1 minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up] unless block ~ ~ ~-1 minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=1] run scoreboard players set @s ra.power.north 1
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=2] run scoreboard players set @s ra.power.north 2
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=3] run scoreboard players set @s ra.power.north 3
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=4] run scoreboard players set @s ra.power.north 4
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=5] run scoreboard players set @s ra.power.north 5
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=6] run scoreboard players set @s ra.power.north 6
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=7] run scoreboard players set @s ra.power.north 7
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=8] run scoreboard players set @s ra.power.north 8
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=9] run scoreboard players set @s ra.power.north 9
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=10] run scoreboard players set @s ra.power.north 10
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=11] run scoreboard players set @s ra.power.north 11
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=12] run scoreboard players set @s ra.power.north 12
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=13] run scoreboard players set @s ra.power.north 13
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=14] run scoreboard players set @s ra.power.north 14
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=side,power=15] run scoreboard players set @s ra.power.north 15
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=1] run scoreboard players set @s ra.power.north 1
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=2] run scoreboard players set @s ra.power.north 2
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=3] run scoreboard players set @s ra.power.north 3
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=4] run scoreboard players set @s ra.power.north 4
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=5] run scoreboard players set @s ra.power.north 5
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=6] run scoreboard players set @s ra.power.north 6
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=7] run scoreboard players set @s ra.power.north 7
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=8] run scoreboard players set @s ra.power.north 8
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=9] run scoreboard players set @s ra.power.north 9
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=10] run scoreboard players set @s ra.power.north 10
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=11] run scoreboard players set @s ra.power.north 11
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=12] run scoreboard players set @s ra.power.north 12
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=13] run scoreboard players set @s ra.power.north 13
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=14] run scoreboard players set @s ra.power.north 14
execute if block ~ ~ ~-1 minecraft:redstone_wire[south=up,power=15] run scoreboard players set @s ra.power.north 15

# South input (dust at z+1 connected north)
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side] unless block ~ ~ ~1 minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up] unless block ~ ~ ~1 minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=1] run scoreboard players set @s ra.power.south 1
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=2] run scoreboard players set @s ra.power.south 2
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=3] run scoreboard players set @s ra.power.south 3
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=4] run scoreboard players set @s ra.power.south 4
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=5] run scoreboard players set @s ra.power.south 5
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=6] run scoreboard players set @s ra.power.south 6
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=7] run scoreboard players set @s ra.power.south 7
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=8] run scoreboard players set @s ra.power.south 8
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=9] run scoreboard players set @s ra.power.south 9
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=10] run scoreboard players set @s ra.power.south 10
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=11] run scoreboard players set @s ra.power.south 11
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=12] run scoreboard players set @s ra.power.south 12
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=13] run scoreboard players set @s ra.power.south 13
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=14] run scoreboard players set @s ra.power.south 14
execute if block ~ ~ ~1 minecraft:redstone_wire[north=side,power=15] run scoreboard players set @s ra.power.south 15
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=1] run scoreboard players set @s ra.power.south 1
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=2] run scoreboard players set @s ra.power.south 2
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=3] run scoreboard players set @s ra.power.south 3
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=4] run scoreboard players set @s ra.power.south 4
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=5] run scoreboard players set @s ra.power.south 5
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=6] run scoreboard players set @s ra.power.south 6
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=7] run scoreboard players set @s ra.power.south 7
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=8] run scoreboard players set @s ra.power.south 8
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=9] run scoreboard players set @s ra.power.south 9
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=10] run scoreboard players set @s ra.power.south 10
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=11] run scoreboard players set @s ra.power.south 11
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=12] run scoreboard players set @s ra.power.south 12
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=13] run scoreboard players set @s ra.power.south 13
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=14] run scoreboard players set @s ra.power.south 14
execute if block ~ ~ ~1 minecraft:redstone_wire[north=up,power=15] run scoreboard players set @s ra.power.south 15

# West input (dust at x-1 connected east)
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side] unless block ~-1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up] unless block ~-1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=1] run scoreboard players set @s ra.power.west 1
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=2] run scoreboard players set @s ra.power.west 2
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=3] run scoreboard players set @s ra.power.west 3
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=4] run scoreboard players set @s ra.power.west 4
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=5] run scoreboard players set @s ra.power.west 5
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=6] run scoreboard players set @s ra.power.west 6
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=7] run scoreboard players set @s ra.power.west 7
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=8] run scoreboard players set @s ra.power.west 8
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=9] run scoreboard players set @s ra.power.west 9
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=10] run scoreboard players set @s ra.power.west 10
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=11] run scoreboard players set @s ra.power.west 11
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=12] run scoreboard players set @s ra.power.west 12
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=13] run scoreboard players set @s ra.power.west 13
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=14] run scoreboard players set @s ra.power.west 14
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=side,power=15] run scoreboard players set @s ra.power.west 15
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=1] run scoreboard players set @s ra.power.west 1
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=2] run scoreboard players set @s ra.power.west 2
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=3] run scoreboard players set @s ra.power.west 3
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=4] run scoreboard players set @s ra.power.west 4
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=5] run scoreboard players set @s ra.power.west 5
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=6] run scoreboard players set @s ra.power.west 6
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=7] run scoreboard players set @s ra.power.west 7
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=8] run scoreboard players set @s ra.power.west 8
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=9] run scoreboard players set @s ra.power.west 9
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=10] run scoreboard players set @s ra.power.west 10
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=11] run scoreboard players set @s ra.power.west 11
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=12] run scoreboard players set @s ra.power.west 12
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=13] run scoreboard players set @s ra.power.west 13
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=14] run scoreboard players set @s ra.power.west 14
execute if block ~-1 ~ ~ minecraft:redstone_wire[east=up,power=15] run scoreboard players set @s ra.power.west 15

# East input (dust at x+1 connected west)
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side] unless block ~1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up] unless block ~1 ~ ~ minecraft:redstone_wire[power=0] run tag @s add ra.powered.dust
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=1] run scoreboard players set @s ra.power.east 1
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=2] run scoreboard players set @s ra.power.east 2
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=3] run scoreboard players set @s ra.power.east 3
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=4] run scoreboard players set @s ra.power.east 4
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=5] run scoreboard players set @s ra.power.east 5
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=6] run scoreboard players set @s ra.power.east 6
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=7] run scoreboard players set @s ra.power.east 7
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=8] run scoreboard players set @s ra.power.east 8
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=9] run scoreboard players set @s ra.power.east 9
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=10] run scoreboard players set @s ra.power.east 10
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=11] run scoreboard players set @s ra.power.east 11
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=12] run scoreboard players set @s ra.power.east 12
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=13] run scoreboard players set @s ra.power.east 13
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=14] run scoreboard players set @s ra.power.east 14
execute if block ~1 ~ ~ minecraft:redstone_wire[west=side,power=15] run scoreboard players set @s ra.power.east 15
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=1] run scoreboard players set @s ra.power.east 1
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=2] run scoreboard players set @s ra.power.east 2
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=3] run scoreboard players set @s ra.power.east 3
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=4] run scoreboard players set @s ra.power.east 4
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=5] run scoreboard players set @s ra.power.east 5
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=6] run scoreboard players set @s ra.power.east 6
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=7] run scoreboard players set @s ra.power.east 7
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=8] run scoreboard players set @s ra.power.east 8
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=9] run scoreboard players set @s ra.power.east 9
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=10] run scoreboard players set @s ra.power.east 10
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=11] run scoreboard players set @s ra.power.east 11
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=12] run scoreboard players set @s ra.power.east 12
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=13] run scoreboard players set @s ra.power.east 13
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=14] run scoreboard players set @s ra.power.east 14
execute if block ~1 ~ ~ minecraft:redstone_wire[west=up,power=15] run scoreboard players set @s ra.power.east 15
