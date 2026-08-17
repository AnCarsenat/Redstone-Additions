# /ra:tools/goggles/billboard/data_line {path,label,color,suffix,y}
# Same as prop_line, but reads from data.status instead of data.properties —
# for values the block computes rather than values the player configures.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {y:$(y),label:"$(label)",value:"N/A",value_color:"red",suffix:"$(suffix)"}
$execute if data entity @s data.status.$(path) run data modify storage ra:temp status_literal.value set from entity @s data.status.$(path)
$execute if data entity @s data.status.$(path) run data modify storage ra:temp status_literal.value_color set value "$(color)"
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
