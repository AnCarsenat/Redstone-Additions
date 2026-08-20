# Allow Ender Fluid Vault to be placed again.
data remove storage ra:settings disabled[{b:"ender_fluid_vault"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Ender Fluid Vault",color:"white"},{text:" enabled.",color:"green"}]
