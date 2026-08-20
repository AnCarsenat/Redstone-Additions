# Extender duration: copy the configured value onto every extender already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.extender]
execute as @e[type=marker,tag=ra.custom_block.extender] run data modify entity @s data.properties.extend set from storage ra:settings global.props."extender"."extend"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing extender: extend = ",color:"gray"},{nbt:"global.props.\"extender\".\"extend\"",storage:"ra:settings",color:"aqua"}]
