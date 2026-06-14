# /ra_multiblock:goggles/show_io_label
# MACRO FUNCTION — Show colored I/O indicator label above a multiblock IO block
# Context: as multiblock marker entity, at base position
# Input: $(x) $(y) $(z) = block offset from base, $(label) = text, $(color) = text color
# Labels are rendered with configurable multiblock offsets.

execute unless data storage ra:display offsets.multiblock_io run function ra:tools/goggles/billboard/init_offsets

$data modify storage ra:temp io set value {x:$(x),y:$(y),z:$(z),label:"$(label)",color:"$(color)"}
data modify storage ra:temp io.ox set from storage ra:display offsets.multiblock_io.x
data modify storage ra:temp io.oy set from storage ra:display offsets.multiblock_io.y
data modify storage ra:temp io.oz set from storage ra:display offsets.multiblock_io.z
data modify storage ra:temp io.scale set from storage ra:display offsets.multiblock_io.scale

function ra:tools/goggles/billboard/show_io_line
