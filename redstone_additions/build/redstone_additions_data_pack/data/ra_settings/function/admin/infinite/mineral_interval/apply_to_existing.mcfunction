# Mineral interval: copy the configured value onto every mineral_generator already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.mineral_generator]
execute as @e[type=marker,tag=ra.custom_block.mineral_generator] run data modify entity @s data.properties.cooldown set from storage ra:settings global.props."mineral_generator"."cooldown"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing mineral_generator: cooldown = ",color:"gray"},{nbt:"global.props.\"mineral_generator\".\"cooldown\"",storage:"ra:settings",color:"aqua"}]
