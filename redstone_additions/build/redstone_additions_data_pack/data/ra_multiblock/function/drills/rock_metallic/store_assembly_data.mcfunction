# /ra_multiblock:drills/rock_metallic/store_assembly_data
# MACRO FUNCTION — Store direction + IO positions after successful structure check
# Called: function ... {dir:"north|south|east|west"}
# Writes to ra:multiblock storage for marker setup

$data modify storage ra:multiblock facing set value "$(dir)"
$data modify storage ra:multiblock inputs set from storage ra:multiblock drill_dir.$(dir).io_inputs
$data modify storage ra:multiblock outputs set from storage ra:multiblock drill_dir.$(dir).io_outputs
$data modify storage ra:multiblock controls set from storage ra:multiblock drill_dir.$(dir).io_controls
