# /ra_chunk_loader:goggles/draw_display_chunk_loader
# Context: as chunk loader armor stand, at block position
# Displays name and status for chunk loader blocks.

data modify storage ra:temp billboard set value {name:"Chunk Loader",show_name:1b,show_status:1b}
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard
