# /ra_wireless:goggles/draw_display_receiver
# Context: as receiver armor stand, at block position

data modify storage ra:temp billboard set value {name:"Wireless Receiver",show_name:1b,show_status:1b}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
