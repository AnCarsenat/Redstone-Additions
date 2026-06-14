# /ra_storage:goggles/draw_display_boxer
# Context: as boxer marker, at block position

data modify storage ra:temp billboard set value {name:"Boxer"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
