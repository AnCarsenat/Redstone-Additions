# /ra_settings:admin_refresh_at {a:N}
# Internal: redraw page N.

data remove storage ra:settings back
$execute if data storage ra:settings apages[$(a)] run data modify storage ra:settings back.f set from storage ra:settings apages[$(a)]
execute if data storage ra:settings back.f run function ra_settings:admin_call with storage ra:settings back
data remove storage ra:settings back
