# /ra_lib:placement/place {block_id:"...",block_tag:"...",dir_type:N}
# Place custom block with marker entity. At position, player tagged ra.placer nearby.
# Output: Block placed, marker with ra.custom_block + ra.custom_block.{tag} + ra.new
# IMPORTANT: Caller must remove ra.new tag after setting properties!
# dir_type: 0 = no facing, 1 = horizontal only, 2 = full 6-directional

$execute as @p[tag=ra.placer,limit=1,sort=nearest] run function ra_lib:orientation/get_facing {dir_type:$(dir_type)}

# Set has_facing flag based on dir_type (0 = no facing needed)
$scoreboard players set #dir_type ra.temp $(dir_type)
data modify storage ra:temp has_facing set value 0b
execute if score #dir_type ra.temp matches 1.. run data modify storage ra:temp has_facing set value 1b

# Get facing name from score
execute if score #facing ra.temp matches 0 run data modify storage ra:temp facing_name set value "down"
execute if score #facing ra.temp matches 1 run data modify storage ra:temp facing_name set value "up"
execute if score #facing ra.temp matches 2 run data modify storage ra:temp facing_name set value "north"
execute if score #facing ra.temp matches 3 run data modify storage ra:temp facing_name set value "south"
execute if score #facing ra.temp matches 4 run data modify storage ra:temp facing_name set value "west"
execute if score #facing ra.temp matches 5 run data modify storage ra:temp facing_name set value "east"

# Place block with or without facing
$data modify storage ra:temp block_id set value "$(block_id)"
function ra_lib:placement/set_block

# Summon marker with initialized data structure
$summon marker ~ ~ ~ {Tags:["ra.custom_block","ra.custom_block.$(block_tag)","ra.new"],data:{_init:1b,properties:{},data:{}}}
data modify entity @e[type=marker,tag=ra.new,distance=..0.1,sort=nearest,limit=1] Rotation set from storage ra:temp Rotation
execute as @p[tag=ra.placer,sort=nearest,limit=1] run scoreboard players operation @e[type=marker,tag=ra.new,distance=..0.1,sort=nearest,limit=1] ra.facing = @s ra.facing

playsound minecraft:block.stone.place block @a[distance=..16] ~ ~ ~ 1 1
