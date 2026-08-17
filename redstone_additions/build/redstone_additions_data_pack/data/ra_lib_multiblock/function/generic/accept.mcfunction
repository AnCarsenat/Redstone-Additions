# /ra_lib_multiblock:generic/accept {type:"...",facing:"..."}
# Internal: publish the winning facing and its IO maps in the shape
# ra_lib_multiblock:setup_marker expects.

$data modify storage ra:multiblock facing set value "$(facing)"
$data modify storage ra:multiblock inputs set from storage ra:multiblock types.$(type).facings.$(facing).inputs
$data modify storage ra:multiblock outputs set from storage ra:multiblock types.$(type).facings.$(facing).outputs
$data modify storage ra:multiblock controls set from storage ra:multiblock types.$(type).facings.$(facing).controls
