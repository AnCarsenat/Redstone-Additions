# Stop Randomizer being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"randomizer"}] run data modify storage ra:settings disabled append value {b:"randomizer"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Randomizer",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
