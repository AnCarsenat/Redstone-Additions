# /ra_ender:blocks/item_vault/cycle_mode
# Cycle shared -> link -> send -> receive. Context: as the vault marker, at the block.
#
#   shared  the contents follow whoever walks up; one real copy, no duplication
#   link    two-way, demand driven: insert here and it goes there, take here and
#           it comes back. For automation on both ends
#   send    one-way out
#   receive one-way in

scoreboard players set #ender.mode ra.temp 0
execute if data entity @s data.properties{mode:"link"} run scoreboard players set #ender.mode ra.temp 1
execute if data entity @s data.properties{mode:"send"} run scoreboard players set #ender.mode ra.temp 2
execute if data entity @s data.properties{mode:"receive"} run scoreboard players set #ender.mode ra.temp 3

execute if score #ender.mode ra.temp matches 0 run data modify entity @s data.properties.mode set value "link"
execute if score #ender.mode ra.temp matches 1 run data modify entity @s data.properties.mode set value "send"
execute if score #ender.mode ra.temp matches 2 run data modify entity @s data.properties.mode set value "receive"
execute if score #ender.mode ra.temp matches 3 run data modify entity @s data.properties.mode set value "shared"

execute if score #ender.mode ra.temp matches 0 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Ender Item Vault: ",color:"gray"},{text:"link (two-way)",color:"light_purple"}]
execute if score #ender.mode ra.temp matches 1 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Ender Item Vault: ",color:"gray"},{text:"send",color:"green"}]
execute if score #ender.mode ra.temp matches 2 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Ender Item Vault: ",color:"gray"},{text:"receive",color:"aqua"}]
execute if score #ender.mode ra.temp matches 3 run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Ender Item Vault: ",color:"gray"},{text:"shared (contents follow you)",color:"yellow"}]
playsound minecraft:block.lever.click block @a[distance=..10] ~ ~ ~ 0.6 1.4
