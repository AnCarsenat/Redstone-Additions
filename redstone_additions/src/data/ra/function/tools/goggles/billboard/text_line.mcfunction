# /ra:tools/goggles/billboard/text_line {label,value,color,suffix,y}
# Render one status line from a literal value.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {y:$(y),label:"$(label)",value:"$(value)",value_color:"$(color)",suffix:"$(suffix)"}
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
