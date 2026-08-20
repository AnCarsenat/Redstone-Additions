# /ra_wires:blocks/electric_furnace/read_mode
# Turn the `mode` property into a speed and a cost.
# Context: as the marker. Writes #ef.speed and #ef.cost in ra.wires.tmp.
#
# Ticks per item, EU per item, and what that works out at per tick:
#
#   low            100 ticks    40 EU     0.4 EU/t   twice a vanilla furnace
#   medium          50 ticks    80 EU     1.6 EU/t
#   high            20 ticks   160 EU     8   EU/t
#   superpowered     5 ticks   300 EU    60   EU/t   forty times vanilla
#
# The EU per item climbs faster than the speed does on purpose. Going four times
# quicker for four times the power would make the lower modes pointless -- there
# would be no reason to ever run anything but superpowered. Paying more than
# linearly for speed is what makes "low" a real choice when your grid is small.
#
# WHAT THE TOP MODE IS PRICED AGAINST
# Superpowered used to cost 1000 EU an item, which is 200 EU/t -- three and a
# third EU Generators running flat out, or about twenty Solar Panels once you
# average a daylight cycle and cover the night. That is not a cost, it is a wall,
# and it made the mode ornamental.
#
# The scale is now anchored on the generators the pack actually ships. One EU
# Generator makes 60 EU/t, so superpowered is exactly one generator flat out --
# a rule you can hold in your head while building. A Solar Panel peaks at 50
# EU/t, so it is a couple of panels at noon and a handful plus a Battery across
# a full day.
#
# The shape is unchanged: each step still costs more per unit of speed than the
# one below it, and low is still much the cheapest way to smelt in bulk -- 7.5x
# less EU per item than superpowered, for a twentieth of the rate.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "low"

scoreboard players set #ef.speed ra.wires.tmp 100
scoreboard players set #ef.cost ra.wires.tmp 40

execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.speed ra.wires.tmp 50
execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.cost ra.wires.tmp 80
execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.speed ra.wires.tmp 20
execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.cost ra.wires.tmp 160
execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.speed ra.wires.tmp 5
execute if data entity @s data.properties{mode:"superpowered"} run scoreboard players set #ef.cost ra.wires.tmp 300

data modify entity @s data.status.mode set from entity @s data.properties.mode
execute store result entity @s data.status.draw int 1 run scoreboard players get #ef.cost ra.wires.tmp
