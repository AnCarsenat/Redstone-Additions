# Enchant crafting: off.
data modify storage ra:settings global."enchanting" set value 0b
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Enchant crafting",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"enchanting\"",storage:"ra:settings",color:"aqua"}]
