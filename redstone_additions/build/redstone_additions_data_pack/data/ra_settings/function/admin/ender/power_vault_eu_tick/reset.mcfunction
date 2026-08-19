# Power Vault EU/tick: back to the shipped default of 80.
data modify storage ra:settings global.props."ender_power_vault"."transfer_rate" set value 80
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Power Vault EU/tick",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"ender_power_vault\".\"transfer_rate\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
