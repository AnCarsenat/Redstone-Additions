# /ra_wires:blocks/creative_fluid/cycle_medium
# Wrench: step the medium round. Context: as the marker, at the block.
#
# Only the media a network can actually be built around. Potions and experience
# are media too, but they arrive by other routes and a creative source for them
# would be a different kind of block.

execute unless data entity @s data.properties.medium run data modify entity @s data.properties.medium set value "water"

scoreboard players set #cr.m ra.wires.tmp 0
execute if data entity @s data.properties{medium:"water"} run scoreboard players set #cr.m ra.wires.tmp 1
execute if data entity @s data.properties{medium:"lava"} run scoreboard players set #cr.m ra.wires.tmp 2
execute if data entity @s data.properties{medium:"powder_snow"} run scoreboard players set #cr.m ra.wires.tmp 3
execute if data entity @s data.properties{medium:"milk"} run scoreboard players set #cr.m ra.wires.tmp 4
execute if data entity @s data.properties{medium:"steam"} run scoreboard players set #cr.m ra.wires.tmp 5

execute if score #cr.m ra.wires.tmp matches 0 run data modify entity @s data.properties.medium set value "water"
execute if score #cr.m ra.wires.tmp matches 1 run data modify entity @s data.properties.medium set value "lava"
execute if score #cr.m ra.wires.tmp matches 2 run data modify entity @s data.properties.medium set value "powder_snow"
execute if score #cr.m ra.wires.tmp matches 3 run data modify entity @s data.properties.medium set value "milk"
execute if score #cr.m ra.wires.tmp matches 4 run data modify entity @s data.properties.medium set value "steam"
execute if score #cr.m ra.wires.tmp matches 5 run data modify entity @s data.properties.medium set value "smoke"

tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Creative source: ",color:"gray"},{nbt:"data.properties.medium",entity:"@s",color:"aqua"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
