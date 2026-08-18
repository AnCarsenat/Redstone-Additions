# /ra_gates:blocks/delayer/process
# Process Delayer logic. As armor stand, at position.
# Delays redstone signal by configurable amount of ticks

# Get delay amount from properties (default 20 ticks = 1 second)
execute unless data entity @s data.properties.delay run data modify entity @s data.properties.delay set value 20

# Detect redstone state for this marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect_switch
tag @s remove ra.redstone.ignore_blocks

# If powered and not counting, start countdown
execute if entity @s[tag=ra.powered] unless entity @s[tag=ra.delaying] run tag @s add ra.delaying
execute if entity @s[tag=ra.delaying] unless data entity @s data.delay_current run function ra_lib:util/property {name:"delay",default:20,min:1}
execute if entity @s[tag=ra.delaying] unless data entity @s data.delay_current store result entity @s data.delay_current int 1 run scoreboard players get #prop ra.temp

# Count down while powered
execute if entity @s[tag=ra.delaying] store result score @s ra.temp run data get entity @s data.delay_current
execute if entity @s[tag=ra.delaying] if score @s ra.temp matches 1.. run scoreboard players remove @s ra.temp 1
execute if entity @s[tag=ra.delaying] store result storage ra:temp delay_val int 1 run scoreboard players get @s ra.temp
execute if entity @s[tag=ra.delaying] run data modify entity @s data.delay_current set from storage ra:temp delay_val

# Output when countdown reaches 0
execute if entity @s[tag=ra.delaying] if score @s ra.temp matches ..0 at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute if entity @s[tag=ra.delaying] if score @s ra.temp matches 1.. at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block

# Reset when power removed
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.delaying] run tag @s remove ra.delaying
execute unless entity @s[tag=ra.powered] run data remove entity @s data.delay_current
execute unless entity @s[tag=ra.powered] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
