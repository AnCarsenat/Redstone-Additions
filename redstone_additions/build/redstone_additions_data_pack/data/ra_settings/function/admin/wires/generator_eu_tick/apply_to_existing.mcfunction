# Generator EU/tick: copy the configured value onto every electric_generator already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.electric_generator]
execute as @e[type=marker,tag=ra.custom_block.electric_generator] run data modify entity @s data.properties.generation_rate set from storage ra:settings global.props."electric_generator"."generation_rate"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing electric_generator: generation_rate = ",color:"gray"},{nbt:"global.props.\"electric_generator\".\"generation_rate\"",storage:"ra:settings",color:"aqua"}]
