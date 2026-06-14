# /ra_interactive:goggles/draw_display_pusher
# Context: as pusher armor stand, at block position
data modify storage ra:temp billboard set value {name:"Pusher"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
