# /ra_lib_multiblock:generic/validate
# Assembly validator for every structure in the registry.
# Hook: #ra_lib_multiblock:validate
# Context: at the multiblock base position; storage ra:multiblock type holds the
# type being attempted.
#
# The hand-written validators (blast_forge, upgrade_platform) run from the same
# tag and answer for their own types; this one only answers for registered ones.

execute if score #mb_result ra.multiblock matches 1 run return 0

data modify storage ra:multiblock scan_q set from storage ra:multiblock registry
function ra_lib_multiblock:generic/validate_next

data remove storage ra:multiblock scan_q
data remove storage ra:multiblock scan
