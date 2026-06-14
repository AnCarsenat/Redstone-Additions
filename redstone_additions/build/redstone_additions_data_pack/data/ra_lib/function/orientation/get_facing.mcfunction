# /ra_lib:orientation/get_facing {dir_type:N}
# Get facing from player rotation. As player.
# Output: @s ra.facing (0-5), storage ra:temp Rotation [yaw,pitch]

execute store result score @s ra.yaw run data get entity @s Rotation[0]
execute store result score @s ra.pitch run data get entity @s Rotation[1]

# Store dir_type
$scoreboard players set #dir_type ra.temp $(dir_type)

# Default: south (3)
scoreboard players set @s ra.facing 3
data modify storage ra:temp Rotation set value [0f, 0f]
execute store result storage ra:temp facing int 1 run scoreboard players set #facing ra.temp 3

execute if score #dir_type ra.temp matches 0 run return run scoreboard players get @s ra.facing

# Vertical (dir_type 2 only)
execute if score #dir_type ra.temp matches 2 if score @s ra.pitch matches 45.. run function ra_lib:orientation/set_facing {f:1,yaw:0f,pitch:-90f}
execute if score #dir_type ra.temp matches 2 if score @s ra.pitch matches 45.. run return run scoreboard players get @s ra.facing
execute if score #dir_type ra.temp matches 2 if score @s ra.pitch matches ..-45 run function ra_lib:orientation/set_facing {f:0,yaw:0f,pitch:90f}
execute if score #dir_type ra.temp matches 2 if score @s ra.pitch matches ..-45 run return run scoreboard players get @s ra.facing

# Horizontal: player looking south (yaw ~0) -> block faces north (toward player)
execute if score @s ra.yaw matches -45..45 run function ra_lib:orientation/set_facing {f:2,yaw:180f,pitch:0f}
execute if score @s ra.yaw matches -45..45 run return run scoreboard players get @s ra.facing
# Player looking west (yaw ~90) -> block faces east
execute if score @s ra.yaw matches 46..135 run function ra_lib:orientation/set_facing {f:5,yaw:-90f,pitch:0f}
execute if score @s ra.yaw matches 46..135 run return run scoreboard players get @s ra.facing
# Player looking east (yaw ~-90) -> block faces west  
execute if score @s ra.yaw matches -135..-46 run function ra_lib:orientation/set_facing {f:4,yaw:90f,pitch:0f}
execute if score @s ra.yaw matches -135..-46 run return run scoreboard players get @s ra.facing

# Player looking north (yaw ~180/-180) -> block faces south
function ra_lib:orientation/set_facing {f:3,yaw:0f,pitch:0f}
return run scoreboard players get @s ra.facing
