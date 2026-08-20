# Magic Crate radius: copy the configured value onto every magic_crate already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.magic_crate]
execute as @e[type=marker,tag=ra.custom_block.magic_crate] run data modify entity @s data.properties.radius set from storage ra:settings global.props."magic_crate"."radius"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing magic_crate: radius = ",color:"gray"},{nbt:"global.props.\"magic_crate\".\"radius\"",storage:"ra:settings",color:"aqua"}]
