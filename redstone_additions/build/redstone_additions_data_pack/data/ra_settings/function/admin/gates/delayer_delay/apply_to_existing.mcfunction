# Delayer delay: copy the configured value onto every delayer already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.delayer]
execute as @e[type=marker,tag=ra.custom_block.delayer] run data modify entity @s data.properties.delay set from storage ra:settings global.props."delayer"."delay"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing delayer: delay = ",color:"gray"},{nbt:"global.props.\"delayer\".\"delay\"",storage:"ra:settings",color:"aqua"}]
