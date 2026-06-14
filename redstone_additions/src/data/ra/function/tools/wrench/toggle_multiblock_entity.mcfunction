# /ra:tools/wrench/toggle_multiblock_entity
# Toggle multiblock enabled state
# Context: as multiblock marker

# Toggle enabled
execute if data entity @s data.properties{enabled:1b} run data modify entity @s data.properties.enabled set value 0b
execute if data entity @s data.properties{enabled:0b} run data modify entity @s data.properties.enabled set value 1b

# Feedback
execute if data entity @s data.properties{enabled:1b} at @s run tellraw @a[distance=..16] [{text:"[Wrench] ",color:"gold"},{text:"Multiblock ",color:"gray"},{text:"enabled",color:"green"}]
execute if data entity @s data.properties{enabled:0b} at @s run tellraw @a[distance=..16] [{text:"[Wrench] ",color:"gold"},{text:"Multiblock ",color:"gray"},{text:"disabled",color:"red"}]

playsound minecraft:block.lever.click block @a[distance=..8] ~ ~ ~ 0.5 1
