# /ra_settings:admin_run {a:N}
# Internal: look action N up in the generated table.

data remove storage ra:settings call
$execute if data storage ra:settings actions[$(a)] run data modify storage ra:settings call.f set from storage ra:settings actions[$(a)]
execute if data storage ra:settings call.f run function ra_settings:admin_call with storage ra:settings call
data remove storage ra:settings call

# Redraw, so the page shows what the click just did. Skipped while an input form
# is open: the value has not been typed yet, and redrawing over the instructions
# would bury them.
execute unless score @s ra.settings.pend matches -1 run function ra_settings:admin_refresh
