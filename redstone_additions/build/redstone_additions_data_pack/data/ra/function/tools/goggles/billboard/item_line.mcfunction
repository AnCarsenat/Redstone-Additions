# /ra:tools/goggles/billboard/item_line {id:"minecraft:iron_ingot",y:0.9}
# Draw one item, floating, as part of a block's goggles readout.
# Context: as the block's marker, at the block position.
#
# The text lines say what a value IS; this says what it LOOKS like, which is the
# only useful way to show an item filter -- "minecraft:polished_blackstone_slab"
# read as text tells you far less than the block does.
#
# Tagged ra.billboard like every other goggles display, so it is torn down and
# redrawn by the same sweep. That matters: a filter shown from a permanent
# display has to be kept in step with the property by its own upkeep, and an
# earlier version of this drew one that was never cleaned up.
#
# Anchored the same way render_literal_line is, so it lines up with the text.

$execute align xyz positioned ~0.5 ~1.3 ~0.5 run summon item_display ~ ~$(y) ~ {item:{id:"$(id)",count:1},item_display:"fixed",billboard:"vertical",brightness:{sky:15,block:15},Tags:["ra","ra.billboard","ra.display"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.4f,0.4f,0.4f]}}
