# /ra_interactive:blocks/block_placer/place_block
# Place block at current position using macro. Input: {id:"minecraft:...",count:N}
# Positioned at target location (^ ^ ^1 from placer)

# Place the block
$setblock ~ ~ ~ $(id)

# Decrement the item count by 1 at the dispenser (^ ^ ^-1 from here)
$scoreboard players set @s ra.temp $(count)
scoreboard players remove @s ra.temp 1
execute if score @s ra.temp matches 1.. positioned ^ ^ ^-1 store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ra.temp
execute if score @s ra.temp matches ..0 positioned ^ ^ ^-1 run data remove block ~ ~ ~ Items[0]
