# /ra:tools/clipboard/apply {props:{...}}
# Internal: merge the copied settings onto this block.
# Context: as the target marker.
#
# A merge rather than a wholesale set, so a property the source block did not
# carry is left alone on the target instead of being deleted. Two blocks of the
# same kind can still differ in what they have if one was placed by an older
# version of the pack.

data merge entity @s {data:{properties:{}}}
data modify entity @s data.properties merge from storage ra:temp clip.props
