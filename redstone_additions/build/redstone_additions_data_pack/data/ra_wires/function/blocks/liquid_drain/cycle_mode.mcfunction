# /ra_wires:blocks/liquid_drain/cycle_mode
# Flip the drain between taking from the world and putting back into it.
# Context: as the marker, at the block.
#
# Lifted out of the goggles tinker when that was removed. Same two-state flip,
# read before write.

execute unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "drain"

scoreboard players set #wr.was ra.temp 0
execute if data entity @s data.properties{mode:"drain"} run scoreboard players set #wr.was ra.temp 1

execute if score #wr.was ra.temp matches 1 run data modify entity @s data.properties.mode set value "place"
execute if score #wr.was ra.temp matches 0 run data modify entity @s data.properties.mode set value "drain"

tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"Drain mode: ",color:"gray"},{nbt:"data.properties.mode",entity:"@s",color:"aqua"}]
playsound minecraft:block.lever.click block @a[distance=..10,scores={ra.u.snd=1..}] ~ ~ ~ 0.6 1.4
