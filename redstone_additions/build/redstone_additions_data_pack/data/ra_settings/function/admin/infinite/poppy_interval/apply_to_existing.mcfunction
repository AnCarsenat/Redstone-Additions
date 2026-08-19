# Poppy interval: copy the configured value onto every poppy_generator already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.poppy_generator]
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] run data modify entity @s data.properties.cooldown set from storage ra:settings global.props."poppy_generator"."cooldown"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing poppy_generator: cooldown = ",color:"gray"},{nbt:"global.props.\"poppy_generator\".\"cooldown\"",storage:"ra:settings",color:"aqua"}]
