# /ra:tools/goggles/billboard/stacked_text_line {label,value,color,suffix}
# text_line without a hand-picked height — see stacked_prop_line.
# Context: as the block marker, at the block position.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {label:"$(label)",value:"$(value)",value_color:"$(color)",suffix:"$(suffix)"}

function ra:tools/goggles/billboard/stack_next
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
