# /ra_interactive:goggles/draw_display_inf_snow
# Context: as infinite_snow_cauldron armor stand, at block position
data modify storage ra:temp billboard set value {name:"Infinite Snow Cauldron"}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
