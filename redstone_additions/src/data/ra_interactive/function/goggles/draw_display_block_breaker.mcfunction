# /ra_interactive:goggles/draw_display_block_breaker
# Context: as block_breaker armor stand, at block position
data modify storage ra:temp billboard set value {name:"Block Breaker"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
