# /ra:tools/goggles/billboard/stacked_status_line {path,label,color,suffix}
# A stacked line reading data.status — values the block computes.
#
# THE NAMING IN HERE IS A TRAP, SO READ THIS ONE
#   prop_line   / stacked_prop_line     -> data.properties  (player configures)
#   data_line                           -> data.status      (block computes)
#   stacked_data_line                   -> data.data        (block's private state)
#   stacked_status_line  (this)         -> data.status
#
# `data_line` and `stacked_data_line` do NOT read the same place, which is easy
# to miss and silent when you get it wrong: the value is simply absent and the
# line renders "N/A". The Electric Furnace shipped with every readout blank for
# exactly that reason -- it wrote data.status and the stacked reader looked in
# data.data. This function is the stacked counterpart of data_line that was
# missing.

data remove storage ra:temp status_literal
$data modify storage ra:temp status_literal set value {label:"$(label)",value:"N/A",value_color:"red",suffix:"$(suffix)"}
$execute if data entity @s data.status.$(path) run data modify storage ra:temp status_literal.value set from entity @s data.status.$(path)
$execute if data entity @s data.status.$(path) run data modify storage ra:temp status_literal.value_color set value "$(color)"

function ra:tools/goggles/billboard/stack_next
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
