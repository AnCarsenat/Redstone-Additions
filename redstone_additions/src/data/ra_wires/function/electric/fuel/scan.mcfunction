# /ra_wires:electric/fuel/scan
# Light the next piece of fuel in the generator's own inventory.
# Context: as the generator marker, at its block. Only called when nothing is
# currently burning.
#
# THIS WALKS THE BARREL, NOT THE REGISTRY
# Two earlier versions of this asked the registry first -- "is there coal in
# there? is there charcoal in there?" -- sixteen questions per attempt, first
# against Items[0] and then with `execute if items ... container.*`. Both were a
# way of testing a container's contents indirectly, and when the generator went
# on reporting no fuel with a full stack of coal in it, there was no way to tell
# from the outside which of the two halves was lying.
#
# So it now reads the container the way the rest of the pack reads containers:
# `data ... Items`, the same access drop_items, the Data Handler and the item
# pipes have always used. Whatever is in slot n is looked up in fuel_map by its
# own id -- one lookup, no search -- and taken out by index. Nothing here depends
# on a command whose behaviour I could not verify from the outside.
#
# It is also cheaper. Before: sixteen tests whatever the barrel held. Now: one
# lookup per stack actually present, which for a generator is nearly always one.

execute unless block ~ ~ ~ minecraft:barrel run return run data modify entity @s data.status.fuel set value "Old block - break and replace"
execute unless data storage ra:wires fuel_map run return run data modify entity @s data.status.fuel set value "Fuel registry missing - /reload"
execute unless data block ~ ~ ~ Items[0] run return 0

data remove storage ra:wires fq
data modify storage ra:wires fq.scan set from block ~ ~ ~ Items
scoreboard players set #gen.i ra.wires.tmp 0
function ra_wires:electric/fuel/slot_next
data remove storage ra:wires fq
