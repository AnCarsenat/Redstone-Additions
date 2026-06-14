# /ra_interactive:goggles/draw_display_message_block
# Context: as message_block armor stand, at block position
data modify storage ra:temp billboard set value {name:"Message Block"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
