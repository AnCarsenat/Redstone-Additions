# /ra:tools/goggles/billboard/show_output
# MACRO FUNCTION — Show red output indicator at IO block position
# Context: as multiblock marker, at base position
# Input: $(label) = text label, $(x) $(y) $(z) = block offset from base

$data modify storage ra:temp io set value {x:$(x),y:$(y),z:$(z),label:"$(label)",color:"red"}
function ra:tools/goggles/billboard/show_io_line
