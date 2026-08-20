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

# Floor, so a ladder can never reach back down into its own block. The anchor is
# 1.3 above the block corner and the block is 1 tall, so anything below -0.3 is
# inside it. A block with more lines than fit now piles the extras on the bottom
# rung, which is ugly but readable — being inside the block was neither.
execute store result score #gg.y ra.temp run data get storage ra:temp status_literal.y 100
execute if score #gg.y ra.temp matches ..-25 run data modify storage ra:temp status_literal.y set value -0.25d

function ra:tools/goggles/billboard/render_literal_line with storage ra:temp status_literal
