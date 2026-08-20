# /ra_settings:disabled_step
# Internal: walk the disabled list.

execute unless data storage ra:settings dscan[0] run return 0
function ra_settings:disabled_row with storage ra:settings dscan[0]
data remove storage ra:settings dscan[0]
function ra_settings:disabled_step
