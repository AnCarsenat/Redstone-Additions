# Clock interval: copy the configured value onto every clock already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.clock]
execute as @e[type=marker,tag=ra.custom_block.clock] run data modify entity @s data.properties.cooldown set from storage ra:settings global.props."clock"."cooldown"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing clock: cooldown = ",color:"gray"},{nbt:"global.props.\"clock\".\"cooldown\"",storage:"ra:settings",color:"aqua"}]
