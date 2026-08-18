# /ra_wires:blocks/electric_consumer/cycle_rate
# Cycle the consumer's draw: normal -> heavy -> light.
# Context: as the consumer marker, at the block.
#
# A consumer takes all or nothing, so this is what it costs to run it for a tick.
# A grid that cannot cover the heavy setting simply leaves the block idle, which
# is the point of having the setting at all: it is how you decide what a base
# spends its generation on when there is not enough to go round.

execute unless data entity @s data.properties.eu_use run data modify entity @s data.properties.eu_use set value 40

scoreboard players set #co.rate ra.wires.tmp 0
execute if data entity @s data.properties{eu_use:40} run scoreboard players set #co.rate ra.wires.tmp 1
execute if data entity @s data.properties{eu_use:80} run scoreboard players set #co.rate ra.wires.tmp 2

execute if score #co.rate ra.wires.tmp matches 0 run data modify entity @s data.properties.eu_use set value 40
execute if score #co.rate ra.wires.tmp matches 1 run data modify entity @s data.properties.eu_use set value 80
execute if score #co.rate ra.wires.tmp matches 2 run data modify entity @s data.properties.eu_use set value 20

execute if score #co.rate ra.wires.tmp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"normal — 40 EU/t",color:"yellow"}]
execute if score #co.rate ra.wires.tmp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"heavy — 80 EU/t",color:"red"}]
execute if score #co.rate ra.wires.tmp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Consumer draw: ",color:"gray"},{text:"light — 20 EU/t",color:"green"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
