# Stop Poppy Generator being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"poppy_generator"}] run data modify storage ra:settings disabled append value {b:"poppy_generator"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Poppy Generator",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
