# /ra_wireless:blocks/emitter/toggle
# Toggle emitter enabled state (called by wrench)

# Store current state in temp
execute store result score @s ra.temp run data get entity @s data.properties.enabled

# Toggle: if 1 -> 0, if 0 -> 1
execute if score @s ra.temp matches 1 run data modify entity @s data.properties.enabled set value 0b
execute if score @s ra.temp matches 0 run data modify entity @s data.properties.enabled set value 1b

# Feedback
execute if data entity @s data.properties{enabled:1b} run tellraw @a[distance=..8] [{text:"[",color:"dark_gray"},{text:"Emitter",color:"yellow"},{text:"] ",color:"dark_gray"},{text:"Enabled",color:"green"}]
execute if data entity @s data.properties{enabled:0b} run tellraw @a[distance=..8] [{text:"[",color:"dark_gray"},{text:"Emitter",color:"yellow"},{text:"] ",color:"dark_gray"},{text:"Disabled",color:"red"}]
playsound minecraft:block.lever.click block @a[distance=..8] ~ ~ ~ 0.5 1.2
