# /ra_settings:admin_run {a:N}
# Internal: look action N up in the generated table.

data remove storage ra:settings call
$execute if data storage ra:settings actions[$(a)] run data modify storage ra:settings call.f set from storage ra:settings actions[$(a)]
execute if data storage ra:settings call.f run function ra_settings:admin_call with storage ra:settings call
data remove storage ra:settings call
