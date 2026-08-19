# Consumer interval: copy the configured value onto every electric_consumer already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.electric_consumer]
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] run data modify entity @s data.properties.cooldown set from storage ra:settings global.props."electric_consumer"."cooldown"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing electric_consumer: cooldown = ",color:"gray"},{nbt:"global.props.\"electric_consumer\".\"cooldown\"",storage:"ra:settings",color:"aqua"}]
