# /ra_interactive:goggles/draw_display_spitter
# Context: as spitter armor stand, at block position
data modify storage ra:temp billboard set value {name:"Spitter"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
