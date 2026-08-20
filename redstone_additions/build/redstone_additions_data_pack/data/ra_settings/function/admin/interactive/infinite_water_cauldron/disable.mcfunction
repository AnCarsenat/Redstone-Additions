# Stop Infinite Water Cauldron being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"infinite_water_cauldron"}] run data modify storage ra:settings disabled append value {b:"infinite_water_cauldron"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Infinite Water Cauldron",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
