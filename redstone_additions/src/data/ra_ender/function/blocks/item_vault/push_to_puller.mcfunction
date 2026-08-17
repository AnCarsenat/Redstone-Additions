# /ra_ender:blocks/item_vault/push_to_puller
# Push one stack specifically to the vault that asked for it.
# Context: as the giving vault marker, at its barrel.

execute unless data block ~ ~ ~ Items[0] run return 0

function ra_ender:blocks/item_vault/where
execute store result storage ra:ender move.src_slot int 1 run data get block ~ ~ ~ Items[0].Slot

data modify storage ra:ender link set value {}
data modify storage ra:ender link.channel set from entity @s data.properties.channel
data modify storage ra:ender link.recv set value "ra.ender.pull_target"
tag @s add ra.ender.sending
function ra_ender:link/send_items with storage ra:ender link
tag @s remove ra.ender.sending

# This end just lost a stack on purpose; record that so it is not read as an
# extraction next cycle, which would have it pull the stack straight back.
function ra_ender:blocks/item_vault/mark
