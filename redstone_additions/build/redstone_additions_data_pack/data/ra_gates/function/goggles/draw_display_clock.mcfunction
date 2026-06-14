# /ra_gates:goggles/draw_display_clock
# Context: as clock armor stand, at block position

data modify storage ra:temp billboard set value {name:"Clock",show_name:1b,show_status:1b}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
