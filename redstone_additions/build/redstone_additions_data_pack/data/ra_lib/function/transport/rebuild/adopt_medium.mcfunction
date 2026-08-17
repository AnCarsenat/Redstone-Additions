# /ra_lib:transport/rebuild/adopt_medium
# Internal: give a network the medium of the contents it inherited.
# When two networks holding different media are joined, the first carrier's
# medium wins and the rest is treated as the same medium rather than vanishing —
# merging two incompatible tanks is a player mistake, not a reason to void metal.

execute unless data entity @s data.data.medium run return 0

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
data modify storage ra:transport arg.medium set from entity @s data.data.medium
function ra_lib:transport/rebuild/adopt_medium_write with storage ra:transport arg
