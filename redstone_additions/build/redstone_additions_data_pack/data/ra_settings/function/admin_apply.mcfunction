# /ra_settings:admin_apply
# Write an operator's typed value into the global setting that was waiting.
# Context: as the operator. Reached from ra_settings:apply_pending on pend = -1.

execute unless data storage ra:settings admin_edit run return 0

data modify storage ra:settings put set from storage ra:settings admin_edit
# A prop target is addressed by block and property rather than by a flat key, so
# it is a different write even though the typed value arrives identically.
execute if data storage ra:settings admin_edit.block run function ra_settings:admin_apply_prop with storage ra:settings put
execute if data storage ra:settings admin_edit.block run data remove storage ra:settings put
execute if data storage ra:settings admin_edit.block run data remove storage ra:settings admin_edit
execute if data storage ra:settings admin_edit.block run return run function ra_settings:admin_refresh

execute if data storage ra:settings admin_edit{type:"int"} run function ra_settings:admin_apply_int with storage ra:settings put
execute if data storage ra:settings admin_edit{type:"str"} run function ra_settings:admin_apply_str with storage ra:settings put

data remove storage ra:settings put
data remove storage ra:settings admin_edit

function ra_settings:admin_refresh
