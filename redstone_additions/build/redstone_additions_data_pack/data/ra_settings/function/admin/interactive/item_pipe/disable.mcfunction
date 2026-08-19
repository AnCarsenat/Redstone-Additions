# Stop Item Pipe being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"item_pipe"}] run data modify storage ra:settings disabled append value {b:"item_pipe"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Item Pipe",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
