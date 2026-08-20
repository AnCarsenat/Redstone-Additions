# Randomizer chance %: down by 5. Applies to blocks placed from now on.
scoreboard players set #n ra.set.tmp 50
execute if data storage ra:settings global.props."randomizer"."chance" store result score #n ra.set.tmp run data get storage ra:settings global.props."randomizer"."chance"
scoreboard players remove #n ra.set.tmp 5
execute if score #n ra.set.tmp matches ..-1 run scoreboard players set #n ra.set.tmp 0
execute if score #n ra.set.tmp matches 101.. run scoreboard players set #n ra.set.tmp 100
execute store result storage ra:settings global.props."randomizer"."chance" int 1 run scoreboard players get #n ra.set.tmp
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Randomizer chance %",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.props.\"randomizer\".\"chance\"",storage:"ra:settings",color:"aqua"},{text:" (new blocks only)",color:"dark_gray"}]
