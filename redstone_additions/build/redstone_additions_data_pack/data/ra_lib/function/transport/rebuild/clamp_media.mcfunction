# /ra_lib:transport/rebuild/clamp_media {id:N}
# Internal: drop the newest medium, or part of it, until the total fits.
# #net_amt_c / #net_cap_c ra.tr.tmp carry the running total and the capacity.
#
# Walked from the tail with media[-1] rather than by rotating the list, because
# rotating reorders it -- and the order is what says which medium arrived first,
# which is what `medium` and every bridge downstream depend on.

execute unless score #net_amt_c ra.tr.tmp > #net_cap_c ra.tr.tmp run return 0
$execute unless data storage ra:transport nets.n$(id).media[-1] run return 0

data remove storage ra:transport clampm
$data modify storage ra:transport clampm.id set value $(id)
$data modify storage ra:transport clampm.medium set from storage ra:transport nets.n$(id).media[-1].m
function ra_lib:transport/rebuild/clamp_one with storage ra:transport clampm

function ra_lib:transport/rebuild/clamp_media with storage ra:transport clampq
