# /ra_wires:electric/consume_steam
# Internal: burn steam from this neighbouring gas network to fuel the generator.
# Context: as a neighbouring fluid node.

execute if score #eu_fuel ra.wires.tmp2 matches 1 run return 0

function ra_lib:transport/net/read
# How much STEAM is here, not how much the network holds. A gas run carrying
# steam and smoke together still fuels a generator; the primary medium is
# whichever arrived first and says nothing about whether steam is present.
execute store result score #eu_steam ra.wires.tmp run data get storage ra:transport cur.amounts.steam
execute unless score #eu_steam ra.wires.tmp matches 20.. run return 0

execute store result score #steam_used ra.wires.tmp run function ra_lib:transport/net/take {amount:20,medium:"steam"}
execute if score #steam_used ra.wires.tmp matches 1.. run scoreboard players set #eu_fuel ra.wires.tmp2 1
