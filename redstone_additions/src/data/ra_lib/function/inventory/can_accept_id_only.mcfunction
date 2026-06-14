# /ra_lib:inventory/can_accept_id_only {id:"..."}
# Set #inv_can_insert when a same-id partial stack exists.

$execute if data block ~ ~ ~ Items[{id:"$(id)",count:..63}] run scoreboard players set #inv_can_insert ra.temp 1
