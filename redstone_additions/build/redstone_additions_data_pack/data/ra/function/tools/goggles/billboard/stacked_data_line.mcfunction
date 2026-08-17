# /ra:tools/goggles/billboard/stacked_data_line {path,label,color,suffix}
# data_line without a hand-picked height — see stacked_prop_line.
# Context: as the block marker, at the block position.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {label:"$(label)",value:"N/A",value_color:"red",suffix:"$(suffix)"}
$execute if data entity @s data.data.$(path) run data modify storage ra:temp status_literal.value set from entity @s data.data.$(path)
$execute if data entity @s data.data.$(path) run data modify storage ra:temp status_literal.value_color set value "$(color)"

function ra:tools/goggles/billboard/stack_next
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
