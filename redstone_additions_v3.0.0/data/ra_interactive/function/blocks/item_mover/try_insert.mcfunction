# /ra_interactive:blocks/item_mover/try_insert
# Try to insert into current output container, then partner half if this is a double chest.
# Inserts one item at a time and accumulates success count.
# Context: positioned at primary output container
# Input: storage ra:temp mover_item
# Output: #mover_inserted ra.temp

scoreboard players set #mover_inserted ra.temp 0
execute store result score @s ra.inv.count run data get storage ra:temp mover_item.count
execute if score @s ra.inv.count matches ..0 run return 0

# Ensure components field exists for macro.
execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}

function ra_interactive:blocks/item_mover/try_insert_loop

return run scoreboard players get #mover_inserted ra.temp
