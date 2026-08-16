# /ra_lib:transport/net/rejoin {class:"fluid"}
# Put a node back into a network it previously left, keeping the capacity it was
# registered with. Context: as the node marker.
#
# Unlike net/join this does not touch ra.tr.cap, so a caller toggling a node in
# and out does not have to remember and restate the node's capacity — getting
# that wrong silently changes how much the whole network can hold.

tag @s add ra.tr.node
scoreboard players set @s ra.tr.net 0
scoreboard players set @s ra.tr.carry 0

$execute if data storage ra:transport classes.$(class) run execute store result score @s ra.tr.class run data get storage ra:transport classes.$(class)

function ra_lib:transport/mark_dirty
