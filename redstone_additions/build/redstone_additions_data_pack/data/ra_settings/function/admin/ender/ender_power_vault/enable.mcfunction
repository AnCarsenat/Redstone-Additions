# Allow Ender Power Vault to be placed again.
data remove storage ra:settings disabled[{b:"ender_power_vault"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Ender Power Vault",color:"white"},{text:" enabled.",color:"green"}]
