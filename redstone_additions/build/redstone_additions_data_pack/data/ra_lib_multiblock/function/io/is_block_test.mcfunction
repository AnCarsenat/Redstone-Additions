# /ra_lib_multiblock:io/is_block_test {match:"..."}
# Internal: the actual comparison, split out so the match string can be a macro.

$execute if block ~ ~ ~ $(match) run scoreboard players set #mb_io ra.multiblock 1
