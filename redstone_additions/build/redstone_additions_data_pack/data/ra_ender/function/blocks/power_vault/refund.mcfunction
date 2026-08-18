# /ra_ender:blocks/power_vault/refund
# Put back the part of a send the far side could not accept.
# Context: as the sending vault marker. Reads #ender.back.
#
# The offer can itself be refused — the local grid may have filled up in the
# meantime, though nothing between the take and here can do that. Whatever comes
# back unaccepted is genuinely lost, and that is the only path in the vault where
# EU can disappear; it needs both grids full at once to happen at all.

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ender.back ra.temp
function ra_wires:electric/offer_eu with storage ra:wires eu
