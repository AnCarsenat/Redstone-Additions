# /ra_lib_multiblock:io/peek {name:"input_1"}
# Copy the first stack of a named IO container to storage ra:multiblock io_item.
# Context: as the marker, at the base. Returns 1 when a stack was found.
# Use this to look at what is waiting before deciding whether to consume it.

data remove storage ra:multiblock io_item
scoreboard players set #mb_io ra.multiblock 0
$function ra_lib_multiblock:io/at {name:"$(name)",run:"ra_lib_multiblock:io/peek_here"}
return run scoreboard players get #mb_io ra.multiblock
