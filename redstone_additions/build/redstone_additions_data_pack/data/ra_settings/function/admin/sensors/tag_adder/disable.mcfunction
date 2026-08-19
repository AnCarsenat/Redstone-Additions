# Stop Tag Adder being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"tag_adder"}] run data modify storage ra:settings disabled append value {b:"tag_adder"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Tag Adder",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
