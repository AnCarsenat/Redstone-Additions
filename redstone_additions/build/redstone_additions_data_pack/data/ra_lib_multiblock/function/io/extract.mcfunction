# /ra_lib_multiblock:io/extract {name:"input_1",id:"minecraft:iron_ore",count:1}
# Take items out of a named IO container. Context: as the marker, at the base.
# Returns 1 when the full amount was removed, 0 otherwise. All-or-nothing: a
# container holding too few leaves its contents untouched.

scoreboard players set #mb_io ra.multiblock 0
$data modify storage ra:multiblock io_req set value {id:"$(id)",count:$(count)}
$function ra_lib_multiblock:io/at {name:"$(name)",run:"ra_lib_multiblock:io/extract_here"}
return run scoreboard players get #mb_io ra.multiblock
