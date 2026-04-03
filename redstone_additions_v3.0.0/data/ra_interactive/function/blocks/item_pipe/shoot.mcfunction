# /ra_interactive:blocks/item_pipe/shoot
# Shoot item out of pipe as entity. As armor stand, at pipe position.
# No valid output - eject the item in front of the pipe.
# Input: storage ra:temp pipe_item = item to shoot

# Spawn item entity in front of pipe (^ ^ ^0.8)
summon item ^ ^ ^0.8 {Item:{id:"minecraft:stone",count:1},Tags:["ra.pipe_shot"],PickupDelay:10s}
data modify entity @e[type=item,tag=ra.pipe_shot,limit=1,sort=nearest] Item set from storage ra:temp pipe_item

# Give it velocity in the facing direction
data modify entity @e[type=item,tag=ra.pipe_shot,limit=1] Motion set value [0.0d,0.0d,0.0d]
execute as @e[type=item,tag=ra.pipe_shot,limit=1] at @s run tp @s ~ ~ ~ ~ ~
execute as @e[type=item,tag=ra.pipe_shot,limit=1] at @s run data modify entity @s Motion set value [0.0d,0.1d,0.0d]

# Apply motion in facing direction using local coords trick
# Summon marker, tp with motion
execute positioned ^ ^ ^0.3 run summon marker ~ ~ ~ {Tags:["ra.motion_helper"]}
execute as @e[type=item,tag=ra.pipe_shot,limit=1] at @e[type=marker,tag=ra.motion_helper,limit=1] run tp @s ~ ~ ~
kill @e[type=marker,tag=ra.motion_helper]

# Remove tag and item from pipe
tag @e[type=item,tag=ra.pipe_shot] remove ra.pipe_shot
data remove block ~ ~ ~ Items[0]
