# Stop Industrial Light being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"industrial_light"}] run data modify storage ra:settings disabled append value {b:"industrial_light"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Industrial Light",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
