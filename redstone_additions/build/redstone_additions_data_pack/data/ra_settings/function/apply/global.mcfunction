# /ra_settings:apply/global {key,type}
# Internal: store a typed value in a world-wide setting.

$execute if data storage ra:settings edit{type:"int"} store result storage ra:settings global."$(key)" int 1 run data get storage ra:input consume.number
$execute if data storage ra:settings edit{type:"str"} run data modify storage ra:settings global."$(key)" set from storage ra:input consume.text
$tellraw @s [{text:"[Settings] ",color:"gold"},{text:"$(key)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"$(key)\"",storage:"ra:settings",color:"aqua"}]
