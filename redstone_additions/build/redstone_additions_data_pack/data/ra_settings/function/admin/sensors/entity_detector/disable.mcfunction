# Stop Entity Detector being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"entity_detector"}] run data modify storage ra:settings disabled append value {b:"entity_detector"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Entity Detector",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
