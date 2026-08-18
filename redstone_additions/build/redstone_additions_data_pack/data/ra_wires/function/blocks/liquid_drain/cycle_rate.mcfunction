# /ra_wires:blocks/liquid_drain/cycle_rate
# Cycle the drain's throughput: slow -> normal -> fast.
# Context: as the drain marker, at the block.
#
# The knob is the cooldown between actions rather than the amount moved, because
# a drain deals in whole source blocks: it either places a bucket's worth or it
# does not. Five litres every 40, 20 or 10 ticks is 2.5, 5 or 10 L a second.

execute unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 20

scoreboard players set #dr.rate ra.wires.tmp 0
execute if data entity @s data.properties{cooldown:20} run scoreboard players set #dr.rate ra.wires.tmp 1
execute if data entity @s data.properties{cooldown:10} run scoreboard players set #dr.rate ra.wires.tmp 2

execute if score #dr.rate ra.wires.tmp matches 0 run data modify entity @s data.properties.cooldown set value 20
execute if score #dr.rate ra.wires.tmp matches 1 run data modify entity @s data.properties.cooldown set value 10
execute if score #dr.rate ra.wires.tmp matches 2 run data modify entity @s data.properties.cooldown set value 40

execute if score #dr.rate ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"normal — 5 L/s",color:"yellow"}]
execute if score #dr.rate ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"fast — 10 L/s",color:"green"}]
execute if score #dr.rate ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain rate: ",color:"gray"},{text:"slow — 2.5 L/s",color:"aqua"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
