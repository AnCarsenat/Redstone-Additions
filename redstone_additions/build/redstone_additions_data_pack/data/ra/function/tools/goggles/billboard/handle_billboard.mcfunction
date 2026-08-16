# /ra:tools/goggles/billboard/handle_billboard
# MACRO FUNCTION — Summon custom block billboard based on per-block config
# Context: as custom block armor stand, at block position
# Input:
#   $(name)     = display name of the block
#   show_name   = optional 1b to render name line
#   name_x/y/z  = optional name line offsets (default from offsets.billboard_name)
#   name_scale  = optional name line scale (default from offsets.billboard_name)

execute unless data storage ra:display offsets.billboard_name run function ra:tools/goggles/billboard/init_offsets

# A caller that supplied a name wants it rendered.
execute unless data storage ra:temp billboard.show_name if data storage ra:temp billboard.name run data modify storage ra:temp billboard.show_name set value 1b
execute unless data storage ra:temp billboard.show_name run return 0

# Optional name line.
execute if data storage ra:temp billboard.show_name run data modify storage ra:temp billboard_render set from storage ra:display offsets.billboard_name
execute if data storage ra:temp billboard.show_name if data storage ra:temp billboard.name_x run data modify storage ra:temp billboard_render.x set from storage ra:temp billboard.name_x
execute if data storage ra:temp billboard.show_name if data storage ra:temp billboard.name_y run data modify storage ra:temp billboard_render.y set from storage ra:temp billboard.name_y
execute if data storage ra:temp billboard.show_name if data storage ra:temp billboard.name_z run data modify storage ra:temp billboard_render.z set from storage ra:temp billboard.name_z
execute if data storage ra:temp billboard.show_name if data storage ra:temp billboard.name_scale run data modify storage ra:temp billboard_render.scale set from storage ra:temp billboard.name_scale
$execute if data storage ra:temp billboard.show_name run data modify storage ra:temp billboard_render.name set value "$(name)"
execute if data storage ra:temp billboard.show_name run function ra:tools/goggles/billboard/render_name with storage ra:temp billboard_render

# No status section here any more. Each block emits its own lines from its
# blocks/<name>/goggles function, using prop_line / data_line / text_line, which
# is what removed the central read_status dispatch table.
