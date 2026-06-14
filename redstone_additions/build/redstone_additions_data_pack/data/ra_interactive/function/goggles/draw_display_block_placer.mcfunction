# /ra_interactive:goggles/draw_display_block_placer
# Context: as block_placer armor stand, at block position
data modify storage ra:temp billboard set value {name:"Block Placer"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
