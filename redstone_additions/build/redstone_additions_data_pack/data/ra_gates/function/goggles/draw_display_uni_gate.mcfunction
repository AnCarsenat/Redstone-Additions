# /ra_gates:goggles/draw_display_uni_gate
# Context: as uni_gate armor stand, at block position
# Shows name + current gate mode

data modify storage ra:temp billboard set value {name:"UNI Gate",show_name:1b,show_status:1b}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
