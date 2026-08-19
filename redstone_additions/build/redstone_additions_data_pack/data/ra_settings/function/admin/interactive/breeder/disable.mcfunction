# Stop Breeder being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"breeder"}] run data modify storage ra:settings disabled append value {b:"breeder"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Breeder",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
