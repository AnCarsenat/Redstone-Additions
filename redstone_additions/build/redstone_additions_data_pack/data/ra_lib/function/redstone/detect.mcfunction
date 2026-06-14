# /ra_lib:redstone/detect
# Detect all redstone power. As entity, at position.
# Output: ra.power (0-16), world dirs (north/south/east/west/up/down),
#         local dirs (front/back/left/right/local_up/local_down),
#         tags ra.powered, ra.powered.{source}, ra.powered.{dir}

function ra_lib:redstone/clear

scoreboard players set @s ra.power 0
scoreboard players set @s ra.power.north 0
scoreboard players set @s ra.power.south 0
scoreboard players set @s ra.power.east 0
scoreboard players set @s ra.power.west 0
scoreboard players set @s ra.power.up 0
scoreboard players set @s ra.power.down 0
scoreboard players set @s ra.power.front 0
scoreboard players set @s ra.power.back 0
scoreboard players set @s ra.power.left 0
scoreboard players set @s ra.power.right 0
scoreboard players set @s ra.power.local_up 0
scoreboard players set @s ra.power.local_down 0

function ra_lib:redstone/detect/dust
function ra_lib:redstone/detect/lever
function ra_lib:redstone/detect/button
execute unless entity @s[tag=ra.redstone.ignore_blocks] run function ra_lib:redstone/detect/block
function ra_lib:redstone/detect/torch
function ra_lib:redstone/detect/repeater
function ra_lib:redstone/detect/comparator

# Max power
execute if score @s ra.power.north > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.north
execute if score @s ra.power.south > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.south
execute if score @s ra.power.east > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.east
execute if score @s ra.power.west > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.west
execute if score @s ra.power.up > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.up
execute if score @s ra.power.down > @s ra.power run scoreboard players operation @s ra.power = @s ra.power.down

# Local direction mapping from world directions using marker facing.
# Facing IDs: 0=down 1=up 2=north 3=south 4=west 5=east
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.front = @s ra.power.down
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.back = @s ra.power.up
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.left = @s ra.power.east
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.right = @s ra.power.west
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.local_up = @s ra.power.south
execute if score @s ra.facing matches 0 run scoreboard players operation @s ra.power.local_down = @s ra.power.north

execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.front = @s ra.power.up
execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.back = @s ra.power.down
execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.left = @s ra.power.east
execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.right = @s ra.power.west
execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.local_up = @s ra.power.north
execute if score @s ra.facing matches 1 run scoreboard players operation @s ra.power.local_down = @s ra.power.south

execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.front = @s ra.power.north
execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.back = @s ra.power.south
execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.left = @s ra.power.west
execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.right = @s ra.power.east
execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.local_up = @s ra.power.up
execute if score @s ra.facing matches 2 run scoreboard players operation @s ra.power.local_down = @s ra.power.down

execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.front = @s ra.power.south
execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.back = @s ra.power.north
execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.left = @s ra.power.east
execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.right = @s ra.power.west
execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.local_up = @s ra.power.up
execute if score @s ra.facing matches 3 run scoreboard players operation @s ra.power.local_down = @s ra.power.down

execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.front = @s ra.power.west
execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.back = @s ra.power.east
execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.left = @s ra.power.south
execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.right = @s ra.power.north
execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.local_up = @s ra.power.up
execute if score @s ra.facing matches 4 run scoreboard players operation @s ra.power.local_down = @s ra.power.down

execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.front = @s ra.power.east
execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.back = @s ra.power.west
execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.left = @s ra.power.north
execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.right = @s ra.power.south
execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.local_up = @s ra.power.up
execute if score @s ra.facing matches 5 run scoreboard players operation @s ra.power.local_down = @s ra.power.down

# Tags
execute if score @s ra.power matches 1.. run tag @s add ra.powered
execute if score @s ra.power matches 16 run tag @s add ra.powered.strong
execute if score @s ra.power.north matches 1.. run tag @s add ra.powered.north
execute if score @s ra.power.south matches 1.. run tag @s add ra.powered.south
execute if score @s ra.power.east matches 1.. run tag @s add ra.powered.east
execute if score @s ra.power.west matches 1.. run tag @s add ra.powered.west
execute if score @s ra.power.up matches 1.. run tag @s add ra.powered.up
execute if score @s ra.power.down matches 1.. run tag @s add ra.powered.down
execute if score @s ra.power.front matches 1.. run tag @s add ra.powered.front
execute if score @s ra.power.back matches 1.. run tag @s add ra.powered.back
execute if score @s ra.power.left matches 1.. run tag @s add ra.powered.left
execute if score @s ra.power.right matches 1.. run tag @s add ra.powered.right
execute if score @s ra.power.local_up matches 1.. run tag @s add ra.powered.local_up
execute if score @s ra.power.local_down matches 1.. run tag @s add ra.powered.local_down

# Return power level
return run scoreboard players get @s ra.power
