# /ra_gates:blocks/extender/process
# Process Extender logic. As armor stand, at position.
# Extends redstone signal duration by configurable amount of ticks

# Get extend amount from properties (default 20 ticks = 1 second)
execute unless data entity @s data.properties.extend run data modify entity @s data.properties.extend set value 20

# Detect redstone state for this marker.
tag @s add ra.redstone.ignore_blocks
function ra_lib:redstone/detect_switch
tag @s remove ra.redstone.ignore_blocks

# Detect rising edge (power goes from 0 to 1+)
execute unless entity @s[tag=ra.was_powered] if entity @s[tag=ra.powered] run tag @s add ra.extending
execute unless entity @s[tag=ra.was_powered] if entity @s[tag=ra.powered] run function ra_lib:util/property {name:"extend",default:20,min:1}
execute unless entity @s[tag=ra.was_powered] if entity @s[tag=ra.powered] store result entity @s data.extend_remaining int 1 run scoreboard players get #prop ra.temp

# Track power state
execute if entity @s[tag=ra.powered] run tag @s add ra.was_powered
execute unless entity @s[tag=ra.powered] run tag @s remove ra.was_powered

# If currently powered, keep resetting the timer
execute if entity @s[tag=ra.powered] run function ra_lib:util/property {name:"extend",default:20,min:1}
execute if entity @s[tag=ra.powered] store result entity @s data.extend_remaining int 1 run scoreboard players get #prop ra.temp

# Output while extending or powered
execute if entity @s[tag=ra.extending] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
execute if entity @s[tag=ra.powered] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block

# Count down when not powered
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] store result score @s ra.temp run data get entity @s data.extend_remaining
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] run scoreboard players remove @s ra.temp 1
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] store result storage ra:temp extend_val int 1 run scoreboard players get @s ra.temp
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] run data modify entity @s data.extend_remaining set from storage ra:temp extend_val

# Stop extending when timer runs out
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] if score @s ra.temp matches ..0 run tag @s remove ra.extending
execute unless entity @s[tag=ra.powered] if entity @s[tag=ra.extending] if score @s ra.temp matches ..0 at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
execute unless entity @s[tag=ra.powered] unless entity @s[tag=ra.extending] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
