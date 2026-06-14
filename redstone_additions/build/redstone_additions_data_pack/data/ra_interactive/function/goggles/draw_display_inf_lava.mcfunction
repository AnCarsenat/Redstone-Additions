# /ra_interactive:goggles/draw_display_inf_lava
# Context: as infinite_lava_cauldron armor stand, at block position
data modify storage ra:temp billboard set value {name:"Infinite Lava Cauldron"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
