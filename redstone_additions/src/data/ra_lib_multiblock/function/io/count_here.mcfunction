# /ra_lib_multiblock:io/count_here
# Internal: positioned at the IO container by io/at.

execute if data block ~ ~ ~ Items store result score #mb_io ra.multiblock run data get block ~ ~ ~ Items
