# Fluid Vault mB/tick: down by 50. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 200
execute if data storage ra:settings global.props."ender_fluid_vault"."transfer_rate" store result score #n ra.set.tmp run data get storage ra:settings global.props."ender_fluid_vault"."transfer_rate"
scoreboard players remove #n ra.set.tmp 50
execute if score #n ra.set.tmp matches ..0 run scoreboard players set #n ra.set.tmp 1
execute if score #n ra.set.tmp matches 10001.. run scoreboard players set #n ra.set.tmp 10000
execute store result storage ra:settings global.props."ender_fluid_vault"."transfer_rate" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Fluid Vault mB/tick",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"ender_fluid_vault\".\"transfer_rate\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
