# /ra_lib_multiblock:io/is_block {name:"redstone_in",block:"minecraft:redstone_block"}
# Test the block sitting at a named IO or control position.
# Context: as the marker, at the base. Returns 1 on a match, 0 otherwise.
# `block` is substituted verbatim, so #block_tags and block states work.
#
#   execute store result score #x obj run function ra_lib_multiblock:io/is_block {name:"redstone_in",block:"minecraft:redstone_block"}

scoreboard players set #mb_io ra.multiblock 0
$data modify storage ra:multiblock io_test set value {match:"$(block)"}
$function ra_lib_multiblock:io/at {name:"$(name)",run:"ra_lib_multiblock:io/is_block_here"}
return run scoreboard players get #mb_io ra.multiblock
