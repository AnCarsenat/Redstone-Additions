# /ra_settings:placement/check_step
# Internal: walk the disabled list against this bat's placement tag.

execute unless data storage ra:settings dscan[0] run return 0
execute if score #blocked ra.set.tmp matches 1 run return 0

function ra_settings:placement/check_one with storage ra:settings dscan[0]

data remove storage ra:settings dscan[0]
function ra_settings:placement/check_step
