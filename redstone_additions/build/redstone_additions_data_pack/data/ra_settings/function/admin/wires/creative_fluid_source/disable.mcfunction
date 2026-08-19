# Stop Creative Fluid Source being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"creative_fluid"}] run data modify storage ra:settings disabled append value {b:"creative_fluid"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Creative Fluid Source",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
