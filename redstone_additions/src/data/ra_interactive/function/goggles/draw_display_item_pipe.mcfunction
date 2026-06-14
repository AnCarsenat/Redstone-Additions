# /ra_interactive:goggles/draw_display_item_pipe
# Context: as item_pipe armor stand, at block position
data modify storage ra:temp billboard set value {name:"Item Pipe"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
