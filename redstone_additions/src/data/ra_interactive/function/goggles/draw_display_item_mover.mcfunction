# /ra_interactive:goggles/draw_display_item_mover
# Context: as item_mover armor stand, at block position
data modify storage ra:temp billboard set value {name:"Item Mover"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
