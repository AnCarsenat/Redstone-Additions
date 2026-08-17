# /ra_lib:transport/net/leave
# Remove this marker from its network and request a rebuild.
# Context: as the node marker. Call on break, before the marker is killed.
#
# The network's contents are parked on its root node between rebuilds, so if the
# node being removed IS the root, the root has to be handed to a survivor first
# or the whole network's contents would disappear with it.

scoreboard players set #handover ra.tr.tmp 0
execute if entity @s[tag=ra.tr.root] run scoreboard players operation #leave_net ra.tr.tmp = @s ra.tr.net
execute if entity @s[tag=ra.tr.root] run scoreboard players set #handover ra.tr.tmp 1

tag @s remove ra.tr.node
tag @s remove ra.tr.root
scoreboard players set @s ra.tr.net 0
scoreboard players set @s ra.tr.class 0

execute if score #handover ra.tr.tmp matches 1 run function ra_lib:transport/net/handover

function ra_lib:transport/mark_dirty
