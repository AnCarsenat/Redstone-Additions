# /ra_infinite:blocks/poppy_generator/cycle_mode
# Flip between single flower and 3×3 patch.
# Context: as the generator's marker, at the block. Called by the wrench.
#
# The old value is read into a score first: writing the property and then testing
# it again in the same function would flip it straight back.

scoreboard players set #poppy.mode ra.temp 0
execute if data entity @s data.properties{mode:"patch"} run scoreboard players set #poppy.mode ra.temp 1

execute if score #poppy.mode ra.temp matches 0 run data modify entity @s data.properties.mode set value "patch"
execute if score #poppy.mode ra.temp matches 1 run data modify entity @s data.properties.mode set value "single"

execute if score #poppy.mode ra.temp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Poppy Generator: ",color:"gray"},{text:"patch",color:"green"}]
execute if score #poppy.mode ra.temp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Poppy Generator: ",color:"gray"},{text:"single",color:"aqua"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
