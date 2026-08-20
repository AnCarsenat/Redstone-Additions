# Stop Ender Power Vault being placed. Blocks already in the world keep working --
# deleting somebody's build because a box was ticked is not a setting.
execute unless data storage ra:settings disabled[{b:"ender_power_vault"}] run data modify storage ra:settings disabled append value {b:"ender_power_vault"}
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Ender Power Vault",color:"white"},{text:" disabled — existing ones keep working.",color:"red"}]
