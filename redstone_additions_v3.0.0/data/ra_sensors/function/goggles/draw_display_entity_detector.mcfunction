# /ra_sensors:goggles/draw_display_entity_detector
# Context: as entity_detector armor stand, at block position

data modify storage ra:temp billboard set value {name:"Entity Detector",show_name:1b,show_status:1b}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
