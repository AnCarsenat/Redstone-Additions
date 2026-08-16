# /ra_lib_multiblock:io/count {name:"input_1"}
# Count the occupied slots of a named IO container.
# Context: as the marker, at the base. Returns the slot count, 0 if empty or if
# the name is unknown.

scoreboard players set #mb_io ra.multiblock 0
$function ra_lib_multiblock:io/at {name:"$(name)",run:"ra_lib_multiblock:io/count_here"}
return run scoreboard players get #mb_io ra.multiblock
