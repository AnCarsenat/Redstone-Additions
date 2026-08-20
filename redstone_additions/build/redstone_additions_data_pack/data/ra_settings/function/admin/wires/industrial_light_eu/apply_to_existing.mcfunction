# Industrial Light EU: copy the configured value onto every industrial_light already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.industrial_light]
execute as @e[type=marker,tag=ra.custom_block.industrial_light] run data modify entity @s data.properties.eu_use set from storage ra:settings global.props."industrial_light"."eu_use"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing industrial_light: eu_use = ",color:"gray"},{nbt:"global.props.\"industrial_light\".\"eu_use\"",storage:"ra:settings",color:"aqua"}]
