# /ra_interactive:goggles/draw_display_inf_water
# Context: as infinite_water_cauldron armor stand, at block position
data modify storage ra:temp billboard set value {name:"Infinite Water Cauldron"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
