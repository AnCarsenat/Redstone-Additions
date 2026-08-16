# /ra_lib_multiblock:io/extract_here
# Internal: positioned at the IO container by io/at.

execute store result score #mb_io ra.multiblock run function ra_lib:inventory/remove with storage ra:multiblock io_req
