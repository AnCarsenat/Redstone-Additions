# Message Block range: copy the configured value onto every message_block already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.message_block]
execute as @e[type=marker,tag=ra.custom_block.message_block] run data modify entity @s data.properties.range set from storage ra:settings global.props."message_block"."range"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing message_block: range = ",color:"gray"},{nbt:"global.props.\"message_block\".\"range\"",storage:"ra:settings",color:"aqua"}]
