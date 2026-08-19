# Stop Message Block being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"message_block"}] run data modify storage ra:settings disabled append value {b:"message_block"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Message Block",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
