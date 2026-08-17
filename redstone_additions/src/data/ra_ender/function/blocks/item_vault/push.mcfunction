# /ra_ender:blocks/item_vault/push
# Hand one stack to the partner on this channel.
# Context: as the vault marker, at the barrel.

execute unless data block ~ ~ ~ Items[0] run return 0

function ra_ender:blocks/item_vault/where
execute store result storage ra:ender move.src_slot int 1 run data get block ~ ~ ~ Items[0].Slot

data modify storage ra:ender link set value {}
data modify storage ra:ender link.channel set from entity @s data.properties.channel
data modify storage ra:ender link.recv set value "ra.ender.recv_item"
tag @s add ra.ender.sending
function ra_ender:link/send_items with storage ra:ender link
tag @s remove ra.ender.sending
