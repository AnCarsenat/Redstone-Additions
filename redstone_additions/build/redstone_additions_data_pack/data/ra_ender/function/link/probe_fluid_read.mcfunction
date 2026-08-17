# /ra_ender:link/probe_fluid_read
# Internal: the partner reports its own network's amount.
# Context: as the partner vault marker.

function ra_lib:transport/net/read
scoreboard players operation #ender.theirs ra.temp = #net_amount ra.tr.tmp
