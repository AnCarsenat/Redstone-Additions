# /ra_lib:transport/net/join {class:"fluid",capacity:N}
# Enrol this marker as a transport node and request a network rebuild.
# Context: as the node marker. Call once, at placement.
#
# `class` is one of "fluid", "item", "electric". Nodes only ever join a network
# of their own class.

tag @s add ra.tr.node
$scoreboard players set @s ra.tr.cap $(capacity)
scoreboard players set @s ra.tr.net 0
scoreboard players set @s ra.tr.carry 0

scoreboard players set @s ra.tr.class 0
$execute if data storage ra:transport classes.$(class) run execute store result score @s ra.tr.class run data get storage ra:transport classes.$(class)

function ra_lib:transport/mark_dirty
