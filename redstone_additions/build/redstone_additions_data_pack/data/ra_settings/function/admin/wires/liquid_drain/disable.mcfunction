# Stop Liquid Drain being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"liquid_drain"}] run data modify storage ra:settings disabled append value {b:"liquid_drain"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Liquid Drain",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
