# Stop Magic Crate being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"magic_crate"}] run data modify storage ra:settings disabled append value {b:"magic_crate"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
