# /ra_gates:goggles/draw_display_extender
# Context: as extender armor stand, at block position
data modify storage ra:temp billboard set value {name:"Extender"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
