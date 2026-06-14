# /ra:tools/goggles/billboard/show_literal_line
# Show a value line from literal storage values
# Context: as source entity, at source position
# Input:
#   y             = vertical offset (relative to centered anchor)
#   x,z           = optional horizontal offsets
#   scale         = optional text scale
#   label         = gray prefix label
#   value         = literal text value to render
#   suffix        = gray suffix text
#   value_color   = color for rendered value

execute unless data storage ra:display offsets.status_line run function ra:tools/goggles/billboard/init_offsets

execute unless data storage ra:temp status_literal.x run data modify storage ra:temp status_literal.x set from storage ra:display offsets.status_line.x
execute unless data storage ra:temp status_literal.y run data modify storage ra:temp status_literal.y set from storage ra:display offsets.status_line.y
execute unless data storage ra:temp status_literal.z run data modify storage ra:temp status_literal.z set from storage ra:display offsets.status_line.z
execute unless data storage ra:temp status_literal.scale run data modify storage ra:temp status_literal.scale set from storage ra:display offsets.status_line.scale

execute unless data storage ra:temp status_literal.label run data modify storage ra:temp status_literal.label set value ""
execute unless data storage ra:temp status_literal.value run data modify storage ra:temp status_literal.value set value "N/A"
execute unless data storage ra:temp status_literal.suffix run data modify storage ra:temp status_literal.suffix set value ""
execute unless data storage ra:temp status_literal.value_color run data modify storage ra:temp status_literal.value_color set value "white"

function ra:tools/goggles/billboard/render_literal_line with storage ra:temp status_literal