# /ra_lib_multiblock:io/insert {name:"output_1",id:"minecraft:iron_ingot",count:1,components:{}}
# Insert an item into a named IO container. Context: as the marker, at the base.
# Returns 1 when the slot exists (the insert itself may still be refused by a
# full container — check ra_lib:inventory/insert's own result if that matters).

$data modify storage ra:multiblock io_item set value {id:"$(id)",count:$(count),components:$(components)}
$return run function ra_lib_multiblock:io/at {name:"$(name)",run:"ra_lib_multiblock:io/insert_here"}
