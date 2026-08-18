# /ra_wires:blocks/electric_furnace/cycle_mode
# Wrench/goggles action: step through the four power modes.
# Context: as the marker, at the block.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "low"

scoreboard players set #ef.m ra.wires.tmp 0
execute if data entity @s data.properties{mode:"low"} run scoreboard players set #ef.m ra.wires.tmp 1
execute if data entity @s data.properties{mode:"medium"} run scoreboard players set #ef.m ra.wires.tmp 2
execute if data entity @s data.properties{mode:"high"} run scoreboard players set #ef.m ra.wires.tmp 3

execute if score #ef.m ra.wires.tmp matches 0 run data modify entity @s data.properties.mode set value "low"
execute if score #ef.m ra.wires.tmp matches 1 run data modify entity @s data.properties.mode set value "medium"
execute if score #ef.m ra.wires.tmp matches 2 run data modify entity @s data.properties.mode set value "high"
execute if score #ef.m ra.wires.tmp matches 3 run data modify entity @s data.properties.mode set value "superpowered"

execute if score #ef.m ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"low - 100 ticks, 40 EU/item",color:"aqua"}]
execute if score #ef.m ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"medium - 50 ticks, 100 EU/item",color:"green"}]
execute if score #ef.m ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"high - 20 ticks, 300 EU/item",color:"yellow"}]
execute if score #ef.m ra.wires.tmp matches 3 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Furnace: ",color:"gray"},{text:"superpowered - 5 ticks, 1000 EU/item",color:"red"}]

playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
