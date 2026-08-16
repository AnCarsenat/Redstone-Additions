# /ra_lib_multiblock:tier_try {type:"...",tier:"..."}
# Internal: attempt one registered type, if it belongs to the requested tier.

$execute unless data storage ra:multiblock types.$(type){tier:"$(tier)"} run return 0

$data modify storage ra:multiblock type set value "$(type)"
execute store result score #mb_tier_done ra.temp run function ra_lib_multiblock:try_assemble
