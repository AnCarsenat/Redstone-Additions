# /ra:tools/goggles/billboard/handle_billboard
# MACRO FUNCTION — Summon centered name billboard above custom block
# Context: as custom block armor stand, at block position
# Input: $(name) = display name of the block

execute unless data storage ra:display offsets.billboard_name run function ra:tools/goggles/billboard/init_offsets

data modify storage ra:temp billboard_render set from storage ra:display offsets.billboard_name
$data modify storage ra:temp billboard_render.name set value "$(name)"
function ra:tools/goggles/billboard/render_name with storage ra:temp billboard_render

# Show block status below the name
function ra:tools/goggles/status/read_status
