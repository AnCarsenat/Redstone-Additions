# /ra_storage:blocks/unboxer/partner
# Internal: the far half of a double chest.
# Context: already positioned at the unboxer's input block.
#
# WHY THESE SIXTEEN LINES LIVE IN THEIR OWN FUNCTION
# They used to sit inline in process, each one a macro line reading
# `$execute positioned $(input1) run execute if score #mover_input_partner ... matches 1 ...`.
# A guard written that way is not a guard: all sixteen commands still run, all
# sixteen still instantiate a macro, and all sixteen still resolve $(input1),
# every pass, even when the input is an ordinary single chest and none of them
# can ever match. Hoisting the condition and the positioning to the one call site
# turns sixteen wasted macro expansions into one comparison.
#
# Which half is which depends on facing AND on type, because a double chest is
# two blocks that each know only their own side.

execute if block ~ ~ ~ minecraft:chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=left] positioned ~1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=north,type=right] positioned ~-1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=left] positioned ~-1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=south,type=right] positioned ~1 ~ ~ run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=left] positioned ~ ~ ~1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=east,type=right] positioned ~ ~ ~-1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=left] positioned ~ ~ ~-1 run function ra_storage:storage_box/empty_crate_here
execute if block ~ ~ ~ minecraft:trapped_chest[facing=west,type=right] positioned ~ ~ ~1 run function ra_storage:storage_box/empty_crate_here
