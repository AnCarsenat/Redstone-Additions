# /ra_settings:apply/user {obj,type,key}
# Internal: store a typed value as this player's own preference.
#
# Numbers go on the objective the row names. Text cannot live on a scoreboard, so
# it goes to storage keyed by UUID -- see ra_settings:apply_str.

$execute if data storage ra:settings edit{type:"int"} store result score @s $(obj) run data get storage ra:input consume.number
execute if data storage ra:settings edit{type:"str"} run function ra_settings:apply_str with storage ra:settings edit
