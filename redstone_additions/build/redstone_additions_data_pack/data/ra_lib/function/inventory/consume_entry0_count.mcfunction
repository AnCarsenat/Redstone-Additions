# /ra_lib:inventory/consume_entry0_count {count:N}
# Consume N items from Items[0] at current container position.
# Returns 1 on success, 0 if slot missing or insufficient count.

execute unless data block ~ ~ ~ Items[0] run return 0

execute store result score @s ra.inv.count run data get block ~ ~ ~ Items[0].count
$scoreboard players set @s ra.temp $(count)

execute if score @s ra.inv.count < @s ra.temp run return 0
scoreboard players operation @s ra.inv.count -= @s ra.temp

execute if score @s ra.inv.count matches 0 run data remove block ~ ~ ~ Items[0]
execute if score @s ra.inv.count matches 1.. store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ra.inv.count

return 1