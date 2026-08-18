# /ra_lib:redstone/clear
# Clear every redstone tag this library has ever set. As the marker.
#
# Two callers. ra_lib:redstone/detect_local runs it each pass, because direction
# tags have to be dropped before they are recomputed. ra:load runs it once over
# every custom block, to sweep up the per-source tags — ra.powered.dust, .lever,
# .block and the rest — that the old detect used to set and nothing ever read.
# They are no longer produced, so without that sweep a block that happened to be
# powered at the moment of the upgrade would carry them forever.
#
# ra_lib:redstone/detect deliberately does NOT call this: it owns exactly two
# tags and removes those two itself, which is most of why it now costs a third of
# what it used to.

tag @s remove ra.powered
tag @s remove ra.powered.strong
tag @s remove ra.powered.repeater
tag @s remove ra.powered.comparator
tag @s remove ra.powered.dust
tag @s remove ra.powered.block
tag @s remove ra.powered.lever
tag @s remove ra.powered.button
tag @s remove ra.powered.torch
tag @s remove ra.powered.north
tag @s remove ra.powered.south
tag @s remove ra.powered.east
tag @s remove ra.powered.west
tag @s remove ra.powered.up
tag @s remove ra.powered.down
tag @s remove ra.powered.front
tag @s remove ra.powered.back
tag @s remove ra.powered.left
tag @s remove ra.powered.right
tag @s remove ra.powered.local_up
tag @s remove ra.powered.local_down
