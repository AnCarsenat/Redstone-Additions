# Allow Ender Item Vault to be placed again.
data remove storage ra:settings disabled[{b:"ender_item_vault"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Ender Item Vault",color:"white"},{text:" enabled.",color:"green"}]
