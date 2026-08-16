# /ra_lib:transport/rebuild/snapshot
# Internal: park one network's contents on its root node.
# Context: as the root marker of a network that is about to be discarded.

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/snapshot_read with storage ra:transport arg
