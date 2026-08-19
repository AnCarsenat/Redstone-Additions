# Stop Creative EU Source being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"creative_eu"}] run data modify storage ra:settings disabled append value {b:"creative_eu"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Creative EU Source",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
