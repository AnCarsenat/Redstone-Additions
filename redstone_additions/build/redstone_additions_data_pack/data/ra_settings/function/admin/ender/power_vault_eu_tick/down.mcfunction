# Power Vault EU/tick: down by 10. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 80
execute if data storage ra:settings global.props."ender_power_vault"."transfer_rate" store result score #n ra.set.tmp run data get storage ra:settings global.props."ender_power_vault"."transfer_rate"
scoreboard players remove #n ra.set.tmp 10
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 10001.. run scoreboard players set #n ra.set.tmp 10000
execute store result storage ra:settings global.props."ender_power_vault"."transfer_rate" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Power Vault EU/tick",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"ender_power_vault\".\"transfer_rate\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
