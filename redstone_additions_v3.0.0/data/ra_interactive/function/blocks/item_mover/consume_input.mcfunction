# /ra_interactive:blocks/item_mover/consume_input
# Remove inserted amount from the first stored entry of the container at current position
# Context: positioned at input container

execute if score #mover_inserted ra.temp matches ..0 run return 0

execute store result storage ra:temp inv_consume.count int 1 run scoreboard players get #mover_inserted ra.temp
return run function ra_lib:inventory/consume_entry0_count with storage ra:temp inv_consume
