# /ra_gates:goggles/draw_display_randomizer
# Context: as randomizer armor stand, at block position
data modify storage ra:temp billboard set value {name:"Randomizer"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
