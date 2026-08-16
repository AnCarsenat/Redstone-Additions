# /ra_lib:inventory/find_free_slot/test {n:N}
# Internal: claim slot n if nothing occupies it.

$execute unless data block ~ ~ ~ Items[{Slot:$(n)b}] run scoreboard players operation #inv_slot ra.temp = #inv_probe ra.temp
