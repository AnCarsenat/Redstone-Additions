# /ra_lib_multiblock:io/at_run {x,y,z,run}
# Internal: offset from the base to the IO block and hand over.

$execute positioned ~$(x) ~$(y) ~$(z) run function $(run)
