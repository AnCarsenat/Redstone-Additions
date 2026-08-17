# /ra_lib_multiblock:generic/validate_type {type:"..."}
# Internal: try all four facings of one registered type at the current position.
# The first facing that matches wins and publishes its IO maps for create_marker.

$execute unless data storage ra:multiblock {type:"$(type)"} run return 0

$execute store result score #mb_result ra.multiblock run function ra_lib_multiblock:structure/check {type:"$(type)",facing:"north"}
$execute if score #mb_result ra.multiblock matches 1 run function ra_lib_multiblock:generic/accept {type:"$(type)",facing:"north"}
execute if score #mb_result ra.multiblock matches 1 run return 1

$execute store result score #mb_result ra.multiblock run function ra_lib_multiblock:structure/check {type:"$(type)",facing:"south"}
$execute if score #mb_result ra.multiblock matches 1 run function ra_lib_multiblock:generic/accept {type:"$(type)",facing:"south"}
execute if score #mb_result ra.multiblock matches 1 run return 1

$execute store result score #mb_result ra.multiblock run function ra_lib_multiblock:structure/check {type:"$(type)",facing:"east"}
$execute if score #mb_result ra.multiblock matches 1 run function ra_lib_multiblock:generic/accept {type:"$(type)",facing:"east"}
execute if score #mb_result ra.multiblock matches 1 run return 1

$execute store result score #mb_result ra.multiblock run function ra_lib_multiblock:structure/check {type:"$(type)",facing:"west"}
$execute if score #mb_result ra.multiblock matches 1 run function ra_lib_multiblock:generic/accept {type:"$(type)",facing:"west"}
execute if score #mb_result ra.multiblock matches 1 run return 1

return 0
