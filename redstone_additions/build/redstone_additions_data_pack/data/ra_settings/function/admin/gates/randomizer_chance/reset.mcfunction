# Randomizer chance %: back to the shipped default of 50.
data modify storage ra:settings global.props."randomizer"."chance" set value 50
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Randomizer chance %",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"randomizer\".\"chance\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
