# /ra_settings:admin_apply_int {key}
# Internal: store a typed number in a global setting.

$execute store result storage ra:settings global."$(key)" int 1 run data get storage ra:input consume.number
$tellraw @s [{text:"[Settings] ",color:"gold"},{text:"$(key)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"$(key)\"",storage:"ra:settings",color:"aqua"}]
