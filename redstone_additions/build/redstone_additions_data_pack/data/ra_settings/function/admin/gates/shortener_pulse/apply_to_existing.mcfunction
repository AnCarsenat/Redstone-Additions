# Shortener pulse: copy the configured value onto every shortener already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.shortener]
execute as @e[type=marker,tag=ra.custom_block.shortener] run data modify entity @s data.properties.pulse set from storage ra:settings global.props."shortener"."pulse"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing shortener: pulse = ",color:"gray"},{nbt:"global.props.\"shortener\".\"pulse\"",storage:"ra:settings",color:"aqua"}]
