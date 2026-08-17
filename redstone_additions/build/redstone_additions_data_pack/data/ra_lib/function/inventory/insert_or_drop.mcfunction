# /ra_lib:inventory/insert_or_drop
# Put storage ra:inventory item into the container at the current position, or
# drop it as an item entity if it will not fit. Returns the number inserted.
#
# Caller contract:
#   data modify storage ra:inventory item set from <your item>
#   function ra_lib:inventory/insert_or_drop
#
# The item is passed through storage rather than as macro arguments on purpose:
# an item's components can carry a player-authored name, and substituting that
# into a command literal breaks on a quote character.
#
# This does NOT use `loot insert`. That command's return value is the number of
# item ENTRIES it handled, not the number of items, so subtracting it from the
# requested count reported almost a whole stack as "left over" — and the stack
# was then inserted AND dropped. The slot is chosen explicitly and the stack is
# appended to the container's Items list instead, which is exact and cannot
# double up.
#
# Trade-off: this claims a free slot rather than topping up a matching partial
# stack. A container with no free slot drops, even if the items would have
# merged. That is predictable, and never duplicates.

scoreboard players set #inv_put ra.temp 0
data remove storage ra:inventory item.Slot

execute store result score #inv_amount ra.temp run data get storage ra:inventory item.count
execute if score #inv_amount ra.temp matches ..0 run return 0

# Not a container at all: nothing to insert into.
execute unless block ~ ~ ~ #ra_lib:containers run function ra_lib:inventory/drop_item
execute unless block ~ ~ ~ #ra_lib:containers run return 0

execute store result score #inv_slot ra.temp run function ra_lib:inventory/find_free_slot
execute if score #inv_slot ra.temp matches ..-1 run function ra_lib:inventory/drop_item
execute if score #inv_slot ra.temp matches ..-1 run return 0

# A container that has never held anything may have no Items list at all.
execute unless data block ~ ~ ~ Items run data modify block ~ ~ ~ Items set value []

data modify storage ra:inventory put set from storage ra:inventory item
execute store result storage ra:inventory put.Slot byte 1 run scoreboard players get #inv_slot ra.temp
data modify block ~ ~ ~ Items append from storage ra:inventory put
data remove storage ra:inventory put

scoreboard players operation #inv_put ra.temp = #inv_amount ra.temp
return run scoreboard players get #inv_put ra.temp
