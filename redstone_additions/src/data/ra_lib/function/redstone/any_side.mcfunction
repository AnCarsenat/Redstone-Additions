# /ra_lib:redstone/any_side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side"}
# Internal: is this one neighbour powering us at all? Returns 1 or 0.
#
# Same sources as ra_lib:redstone/side, but every test bails out the moment it
# hits, and none of them resolves a level. The expensive part of reading redstone
# is working out *how much* — fifteen state comparisons for a dust or a weighted
# plate — and a caller that only wants to know whether it is on should never pay
# for that. Testing dust against power=0 answers "carrying anything" in one
# comparison instead of fifteen.
#
# Ordered cheapest and commonest first, so the usual answer costs one or two
# tests. Cost is only fully paid on a side with nothing on it.

scoreboard players set #rs ra.temp 0

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/binary_sources[powered=true] run return 1
$execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_block run return 1

$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=side] unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[power=0] run return 1
$execute if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[$(back)=up] unless block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_wire[power=0] run return 1

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/directional_sources[facing=$(back),powered=true] run return 1

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_omni unless block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_omni[power=0] run return 1

# Torch rules are per-side and already written down once; reuse them rather than
# restate them, and read the level they leave behind as a yes or no.
$function ra_lib:redstone/torch/$(torch) {dx:$(dx),dy:$(dy),dz:$(dz),side:"$(side)"}
execute if score #rs ra.temp matches 1.. run return 1

return 0
