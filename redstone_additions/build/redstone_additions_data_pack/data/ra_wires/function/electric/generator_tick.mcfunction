# /ra_wires:electric/generator_tick
# Burn fuel and put the EU it makes onto the grid.
# Context: as the generator marker, at its position.
#
# THE BLOCK IS A BARREL WEARING A FURNACE
# It used to be a real blast furnace, which meant the block you interacted with
# had a furnace's own UI: two input slots and a fuel slot, of which only the fuel
# slot meant anything. Players quite reasonably put ore in the top and waited.
# A barrel is a plain inventory — drop coal in and that is the whole interaction —
# and ra_lib:skin/apply puts the furnace back on the outside so it still reads as
# one. See that function for why mechanics and appearance are separable here.
#
# Steam still works. A generator piped into a boiler's steam network burns that
# instead, so the water -> Boiler -> steam -> EU chain is intact; solid fuel is
# the direct route, steam is the built one.

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute if data entity @s data.properties{enabled:0b} run return 0

function ra_lib:util/property {name:"generation_rate",default:60,min:1}
scoreboard players operation #gen.rate ra.wires.tmp = #prop ra.temp

# --- Solid fuel, out of the barrel ---
scoreboard players set #gen.burning ra.wires.tmp 0

# Nothing alight: try to light something.
execute unless data entity @s data.data.burn run function ra_wires:electric/fuel/scan

execute if data entity @s data.data.burn run scoreboard players set #gen.burning ra.wires.tmp 1
execute if score #gen.burning ra.wires.tmp matches 1 store result score #gen.left ra.wires.tmp run data get entity @s data.data.burn 1
execute if score #gen.burning ra.wires.tmp matches 1 run scoreboard players remove #gen.left ra.wires.tmp 1
execute if score #gen.burning ra.wires.tmp matches 1 store result entity @s data.data.burn int 1 run scoreboard players get #gen.left ra.wires.tmp
execute if score #gen.burning ra.wires.tmp matches 1 if score #gen.left ra.wires.tmp matches ..0 run data remove entity @s data.data.burn

# --- Steam, out of an adjacent gas network ---
scoreboard players set #eu_fuel ra.wires.tmp2 0
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #gen.burning ra.wires.tmp matches 0 positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute if score #eu_fuel ra.wires.tmp2 matches 1.. run scoreboard players set #gen.burning ra.wires.tmp 1
execute if score #eu_fuel ra.wires.tmp2 matches 1.. run data modify entity @s data.status.fuel set value "Steam"

execute if score #gen.burning ra.wires.tmp matches 0 run data modify entity @s data.status.fuel set value "No fuel"
execute if score #gen.burning ra.wires.tmp matches 0 run data modify entity @s data.status.active set value 0b
execute if score #gen.burning ra.wires.tmp matches 0 run return 0

# Offer this tick's output to the grid. The network decides how much it can hold
# and hands back the rest as a refusal — there is no buffer here to overflow.
execute store result storage ra:wires eu.amount int 1 run scoreboard players get #gen.rate ra.wires.tmp
execute store result score #eu_made ra.wires.tmp run function ra_wires:electric/offer_eu with storage ra:wires eu

execute if score #eu_made ra.wires.tmp matches 1.. run data modify entity @s data.status.active set value 1b
execute if score #eu_made ra.wires.tmp matches ..0 run data modify entity @s data.status.active set value 0b
