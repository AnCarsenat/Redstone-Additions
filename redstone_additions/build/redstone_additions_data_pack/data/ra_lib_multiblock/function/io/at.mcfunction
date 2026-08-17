# /ra_lib_multiblock:io/at {name:"input_1",run:"namespace:function"}
# Run a function positioned at one of this multiblock's named IO blocks.
# Context: as the multiblock marker, at the base position.
# Returns 1 when the slot exists, 0 when the name is unknown.
#
# The name is looked up in the marker's own data.inputs / data.outputs /
# data.controls, so this works for any multiblock that went through setup_marker,
# including the hand-written ones. Callers never need to know the facing or redo
# the offset maths — that was the main reason every multiblock ended up with four
# near-identical copies of its processing logic.
#
#   function ra_lib_multiblock:io/at {name:"output_1",run:"ra_multiblock:blast_forge/emit"}

data remove storage ra:multiblock io_pos

$execute if data entity @s data.inputs.$(name) run data modify storage ra:multiblock io_pos set from entity @s data.inputs.$(name)
$execute if data entity @s data.outputs.$(name) run data modify storage ra:multiblock io_pos set from entity @s data.outputs.$(name)
$execute if data entity @s data.controls.$(name) run data modify storage ra:multiblock io_pos set from entity @s data.controls.$(name)

execute unless data storage ra:multiblock io_pos run return 0

$data modify storage ra:multiblock io_pos.run set value "$(run)"
function ra_lib_multiblock:io/at_run with storage ra:multiblock io_pos
return 1
