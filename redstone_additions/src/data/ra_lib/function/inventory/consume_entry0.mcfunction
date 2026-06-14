# /ra_lib:inventory/consume_entry0
# Consume one item from Items[0] at current container position.
# Returns 1 on success, 0 if slot empty.

execute unless data block ~ ~ ~ Items[0] run return 0

execute store result score @s ra.inv.count run data get block ~ ~ ~ Items[0].count

execute if score @s ra.inv.count matches 1 run data remove block ~ ~ ~ Items[0]
execute if score @s ra.inv.count matches 1 run return 1

scoreboard players remove @s ra.inv.count 1
execute store result block ~ ~ ~ Items[0].count int 1 run scoreboard players get @s ra.inv.count
return 1
