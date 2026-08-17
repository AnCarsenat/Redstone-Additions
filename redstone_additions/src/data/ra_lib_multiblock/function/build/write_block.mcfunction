# /ra_lib_multiblock:build/write_block {type,facing,x,y,z,match}
# Internal: append one rotated block requirement, with its position baked into
# the relative-coordinate string the structure checker feeds to `if block`.

$data modify storage ra:multiblock types.$(type).facings.$(facing).blocks append value {pos:"~$(x) ~$(y) ~$(z)",match:"$(match)"}
