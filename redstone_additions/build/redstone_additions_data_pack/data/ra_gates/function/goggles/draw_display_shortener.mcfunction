# /ra_gates:goggles/draw_display_shortener
# Context: as shortener armor stand, at block position
data modify storage ra:temp billboard set value {name:"Shortener"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
