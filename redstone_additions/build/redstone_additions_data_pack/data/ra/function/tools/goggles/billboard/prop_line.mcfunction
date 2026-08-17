# /ra:tools/goggles/billboard/prop_line {path,label,color,suffix,y}
# Render one status line from a property on this block's marker.
# Context: as the block marker, at the block position.
#
# A missing property renders "N/A" in red rather than being skipped, so a block
# that has not been configured yet says so instead of silently showing nothing.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {y:$(y),label:"$(label)",value:"N/A",value_color:"red",suffix:"$(suffix)"}
$execute if data entity @s data.properties.$(path) run data modify storage ra:temp status_literal.value set from entity @s data.properties.$(path)
$execute if data entity @s data.properties.$(path) run data modify storage ra:temp status_literal.value_color set value "$(color)"
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
