# Liquid Drain interval: copy the configured value onto every liquid_drain already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.liquid_drain]
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] run data modify entity @s data.properties.cooldown set from storage ra:settings global.props."liquid_drain"."cooldown"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing liquid_drain: cooldown = ",color:"gray"},{nbt:"global.props.\"liquid_drain\".\"cooldown\"",storage:"ra:settings",color:"aqua"}]
