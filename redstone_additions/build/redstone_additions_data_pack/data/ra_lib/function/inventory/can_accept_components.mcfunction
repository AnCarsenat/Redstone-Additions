# /ra_lib:inventory/can_accept_components {id:"...",components:{...}}
# Set #inv_can_insert when a compatible partial stack exists.

$execute if data block ~ ~ ~ Items[{id:"$(id)",components:$(components),count:..63}] run scoreboard players set #inv_can_insert ra.temp 1
