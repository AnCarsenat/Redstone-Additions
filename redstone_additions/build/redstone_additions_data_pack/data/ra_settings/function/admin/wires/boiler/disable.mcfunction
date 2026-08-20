# Stop Boiler being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"boiler"}] run data modify storage ra:settings disabled append value {b:"boiler"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Boiler",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
