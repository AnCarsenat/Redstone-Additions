# /ra_ender:link/probe_power_read
# Internal: read this vault's grid total into #ender.theirs.
# Context: as the partner vault marker.
#
# Split out because net/read leaves its answer in a scratch score rather than
# returning it, so the caller's `store result` had nothing to capture.

function ra_lib:transport/net/read
scoreboard players operation #ender.theirs ra.temp = #net_amount ra.tr.tmp
