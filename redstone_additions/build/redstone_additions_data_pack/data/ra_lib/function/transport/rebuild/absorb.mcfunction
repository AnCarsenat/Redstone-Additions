# /ra_lib:transport/rebuild/absorb
# Internal: fold one node's carried contents into the network it landed in,
# medium by medium. Context: as a node marker carrying something.
#
# Replaces adopt_medium, which could only name the network after the fact and had
# to pretend everything it inherited was that one medium. Two runs joined by a
# new pipe now arrive as two sets of per-medium figures that add up, and a run
# that carried water and lava still carries water and lava afterwards.
#
# The old function is still called for a node carrying a total with no breakdown
# behind it, which is the only case left that cannot be split up.

execute unless data entity @s data.data.carry[0] run return run function ra_lib:transport/rebuild/adopt_medium

data remove storage ra:transport absq
execute store result storage ra:transport absq.id int 1 run scoreboard players get @s ra.tr.net
data modify storage ra:transport absq.queue set from entity @s data.data.carry

# The first carrier to arrive with a potion sets the network's, matching the rule
# the media list follows: what got there first is what stays.
execute if data entity @s data.data.carry_potion run data modify storage ra:transport absq.potion set from entity @s data.data.carry_potion
execute if data storage ra:transport absq.potion run function ra_lib:transport/rebuild/absorb_potion with storage ra:transport absq

function ra_lib:transport/rebuild/absorb_next

# Insurance. If every carried entry came through holding nothing, the network is
# left nameless -- and a run reporting "Empty" over a non-zero Amount is the most
# confusing thing this code can produce. adopt_medium only writes when there is
# no medium yet, so this costs one test in the normal case.
function ra_lib:transport/rebuild/adopt_medium

data remove entity @s data.data.carry
data remove entity @s data.data.carry_potion
