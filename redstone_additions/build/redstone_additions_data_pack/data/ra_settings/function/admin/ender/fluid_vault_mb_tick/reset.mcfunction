# Fluid Vault mB/tick: back to the shipped default of 200.
data modify storage ra:settings global.props."ender_fluid_vault"."transfer_rate" set value 200
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Fluid Vault mB/tick",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"ender_fluid_vault\".\"transfer_rate\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
