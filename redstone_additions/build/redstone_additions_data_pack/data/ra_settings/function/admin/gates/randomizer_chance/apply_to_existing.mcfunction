# Randomizer chance %: copy the configured value onto every randomizer already placed.
# Overwrites any per-block value set with the wrench.
scoreboard players set #n ra.set.tmp 0
execute store result score #n ra.set.tmp if entity @e[type=marker,tag=ra.custom_block.randomizer]
execute as @e[type=marker,tag=ra.custom_block.randomizer] run data modify entity @s data.properties.chance set from storage ra:settings global.props."randomizer"."chance"
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Updated ",color:"gray"},{score:{name:"#n",objective:"ra.set.tmp"},color:"aqua"},{text:" existing randomizer: chance = ",color:"gray"},{nbt:"global.props.\"randomizer\".\"chance\"",storage:"ra:settings",color:"aqua"}]
