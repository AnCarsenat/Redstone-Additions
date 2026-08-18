# /ra_wires:blocks/electric_furnace/cycle_output
# Wrench/goggles action: step the output face round under -> front -> back -> top.
# Context: as the marker, at the block.
#
# One of three settings this block declares in ra:tools/wrench/init_registry, so
# the wrench offers it in a menu alongside the power mode and the enable switch
# rather than each living on a different tool.

execute unless data entity @s data.properties.output run data modify entity @s data.properties.output set value "under"

scoreboard players set #ef.o ra.wires.tmp 0
execute if data entity @s data.properties{output:"under"} run scoreboard players set #ef.o ra.wires.tmp 1
execute if data entity @s data.properties{output:"front"} run scoreboard players set #ef.o ra.wires.tmp 2
execute if data entity @s data.properties{output:"back"} run scoreboard players set #ef.o ra.wires.tmp 3

execute if score #ef.o ra.wires.tmp matches 0 run data modify entity @s data.properties.output set value "under"
execute if score #ef.o ra.wires.tmp matches 1 run data modify entity @s data.properties.output set value "front"
execute if score #ef.o ra.wires.tmp matches 2 run data modify entity @s data.properties.output set value "back"
execute if score #ef.o ra.wires.tmp matches 3 run data modify entity @s data.properties.output set value "top"

# Wrench actions message everyone nearby. The wrench runs `as` the marker off a
# raycast and never touches the player, so anything addressed to a player-side
# tag would reach nobody -- which is exactly how this once cycled in silence.
tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace output: ",color:"gray"},{nbt:"data.properties.output",entity:"@s",color:"green"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
