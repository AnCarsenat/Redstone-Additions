# /ra_settings:admin_apply_str {key}
# Internal: store typed text in a global setting.

$data modify storage ra:settings global."$(key)" set from storage ra:input consume.text
$tellraw @s [{text:"[Settings] ",color:"gold"},{text:"$(key)",color:"white"},{text:" is now ",color:"gray"},{nbt:"global.\"$(key)\"",storage:"ra:settings",color:"aqua"}]
