# Stop Nether Generator being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"nether_generator"}] run data modify storage ra:settings disabled append value {b:"nether_generator"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Nether Generator",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
