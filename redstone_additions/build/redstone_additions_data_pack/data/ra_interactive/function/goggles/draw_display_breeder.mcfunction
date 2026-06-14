# /ra_interactive:goggles/draw_display_breeder
# Context: as breeder armor stand, at block position
data modify storage ra:temp billboard set value {name:"Breeder"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
