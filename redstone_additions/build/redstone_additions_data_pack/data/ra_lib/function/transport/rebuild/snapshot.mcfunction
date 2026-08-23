# /ra_lib:transport/rebuild/snapshot
# Internal: park one network's contents on its root node.
# Context: as the root marker of a network that is about to be discarded.

# The number goes on the node with the contents, so rebuild/seed can give it back
# to whatever component this node ends up in. The counter is dragged up past it
# at the same time: a fresh id must never collide with one that is about to be
# inherited, and after this pass the counter is above every id in use.
scoreboard players operation @s ra.tr.old = @s ra.tr.net
execute if score @s ra.tr.net > #net_seq ra.tr.tmp run scoreboard players operation #net_seq ra.tr.tmp = @s ra.tr.net

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/snapshot_read with storage ra:transport arg
