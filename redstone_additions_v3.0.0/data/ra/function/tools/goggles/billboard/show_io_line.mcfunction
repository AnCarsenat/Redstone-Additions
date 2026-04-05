# /ra:tools/goggles/billboard/show_io_line
# Render one colored I/O label from storage ra:temp io.

execute unless data storage ra:display offsets.io_label run function ra:tools/goggles/billboard/init_offsets

execute unless data storage ra:temp io.x run data modify storage ra:temp io.x set value 0
execute unless data storage ra:temp io.y run data modify storage ra:temp io.y set value 0
execute unless data storage ra:temp io.z run data modify storage ra:temp io.z set value 0
execute unless data storage ra:temp io.label run data modify storage ra:temp io.label set value "I/O"
execute unless data storage ra:temp io.color run data modify storage ra:temp io.color set value "white"

execute unless data storage ra:temp io.ox run data modify storage ra:temp io.ox set from storage ra:display offsets.io_label.x
execute unless data storage ra:temp io.oy run data modify storage ra:temp io.oy set from storage ra:display offsets.io_label.y
execute unless data storage ra:temp io.oz run data modify storage ra:temp io.oz set from storage ra:display offsets.io_label.z
execute unless data storage ra:temp io.scale run data modify storage ra:temp io.scale set from storage ra:display offsets.io_label.scale

function ra:tools/goggles/billboard/render_io_line with storage ra:temp io
