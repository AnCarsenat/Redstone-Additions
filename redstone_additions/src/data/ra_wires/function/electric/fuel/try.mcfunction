# /ra_wires:electric/fuel/try {item:"minecraft:coal",ticks:N}
# Internal: one registry entry against the stack in the generator.
# Context: as the generator marker, at its block.
#
# The item is taken first and the burn is only set if it was actually taken, so a
# failed consume cannot hand out free power.

$execute unless data block ~ ~ ~ Items[0]{id:"$(item)"} run return 0

execute store result score #gen.took ra.wires.tmp run function ra_lib:inventory/consume_entry0
execute if score #gen.took ra.wires.tmp matches ..0 run return 0

$data modify entity @s data.data.burn set value $(ticks)
$data modify entity @s data.status.fuel set value "$(item)"

playsound minecraft:block.furnace.fire_crackle block @a[distance=..12] ~ ~ ~ 0.4 1.2
