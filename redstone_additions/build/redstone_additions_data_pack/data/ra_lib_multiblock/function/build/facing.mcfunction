# /ra_lib_multiblock:build/facing {type:"...",facing:"..."}
# Internal: rotate one structure's block list and IO lists into one facing.
# #mb_rot selects the rotation: 0 north, 1 south, 2 east, 3 west.

data modify storage ra:multiblock work.type set from storage ra:multiblock build.type
data modify storage ra:multiblock work.facing set from storage ra:multiblock build.facing

# Block pattern.
$data modify storage ra:multiblock types.$(type).facings.$(facing).blocks set value []
$data modify storage ra:multiblock work_q set from storage ra:multiblock types.$(type).blocks
data modify storage ra:multiblock work.kind set value "blocks"
function ra_lib_multiblock:build/entry_next

# IO maps. These end up as compounds keyed by name, which is the shape
# setup_marker copies onto the marker and the shape the io/ helpers read.
$data modify storage ra:multiblock types.$(type).facings.$(facing).inputs set value {}
$data modify storage ra:multiblock types.$(type).facings.$(facing).outputs set value {}
$data modify storage ra:multiblock types.$(type).facings.$(facing).controls set value {}

$data modify storage ra:multiblock work_q set from storage ra:multiblock types.$(type).inputs
data modify storage ra:multiblock work.kind set value "inputs"
function ra_lib_multiblock:build/entry_next

$data modify storage ra:multiblock work_q set from storage ra:multiblock types.$(type).outputs
data modify storage ra:multiblock work.kind set value "outputs"
function ra_lib_multiblock:build/entry_next

$data modify storage ra:multiblock work_q set from storage ra:multiblock types.$(type).controls
data modify storage ra:multiblock work.kind set value "controls"
function ra_lib_multiblock:build/entry_next
