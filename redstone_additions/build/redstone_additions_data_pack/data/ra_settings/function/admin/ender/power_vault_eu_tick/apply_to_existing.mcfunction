# Power Vault EU/tick: copy the configured value onto every ender_power_vault already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.ender_power_vault]
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] run data modify entity @s data.properties.transfer_rate set from storage ra:settings global.props."ender_power_vault"."transfer_rate"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing ender_power_vault: transfer_rate = ",color:"gray"},{nbt:"global.props.\"ender_power_vault\".\"transfer_rate\"",storage:"ra:settings",color:"aqua"}]
