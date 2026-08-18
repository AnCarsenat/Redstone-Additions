# /ra_wires:blocks/electric_furnace/read_mode
# Turn the `mode` property into a speed and a cost.
# Context: as the marker. Writes #ef.speed and #ef.cost in ra.wires.tmp.
#
# Ticks per item, and EU per item:
#
#   low            100 ticks    40 EU     twice a vanilla furnace, cheap
#   medium          50 ticks   100 EU
#   high            20 ticks   300 EU
#   superpowered     5 ticks  1000 EU     forty times vanilla
#
# The EU per item climbs faster than the speed does on purpose. Going four times
# quicker for four times the power would make the lower modes pointless -- there
# would be no reason to ever run anything but superpowered. Paying more than
# linearly for speed is what makes "low" a real choice when your grid is small.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "low"

scoreboard players set #ef.speed ra.wires.tmp 100
scoreboard players set #ef.cost ra.wires.tmp 40

execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.speed ra.wires.tmp 50
execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.cost ra.wires.tmp 100
execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.speed ra.wires.tmp 20
execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.cost ra.wires.tmp 300
execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.speed ra.wires.tmp 5
execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.cost ra.wires.tmp 1000

data modify entity @s data.status.mode set from entity @s data.properties.mode
execute store result entity @s data.status.draw int 1 run scoreboard players get #ef.cost ra.wires.tmp
