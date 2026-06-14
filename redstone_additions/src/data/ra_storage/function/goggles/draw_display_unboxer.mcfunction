# /ra_storage:goggles/draw_display_unboxer
# Context: as unboxer marker, at block position

data modify storage ra:temp billboard set value {name:"Unboxer"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
