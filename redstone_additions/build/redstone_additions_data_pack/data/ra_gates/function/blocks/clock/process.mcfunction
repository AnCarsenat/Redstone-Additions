# /ra_gates:blocks/clock/process
# Process Clock gate logic. As armor stand, at position.

execute at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 iron_block replace redstone_block
$execute if score @s ra.cooldown matches ..$(cooldown) run return run scoreboard players add @s ra.cooldown 1
execute at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 redstone_block replace iron_block
scoreboard players set @s ra.cooldown 0
