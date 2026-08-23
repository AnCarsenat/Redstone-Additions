# /ra_lib:redstone/has_input {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side",dust:"side"}
# Internal: is there a redstone component on this side that could power us,
# whether or not it currently is? Returns 1 or 0.
#
# The presence twin of ra_lib:redstone/any_side. Same sources, same orientation
# rules, same opt-out — a component only counts where any_side would be able to
# read power from it — but with every powered/lit test dropped. That is the whole
# point: it lets a caller tell "an input is here and it is off" apart from "there
# is no input on this side", which an AND gate needs and a power reading cannot
# give it.

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/binary_sources run return 1
$execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_block run return 1

$execute store result score #rs.dust ra.temp run function ra_lib:redstone/dust_present/$(dust) {dx:$(dx),dy:$(dy),dz:$(dz),back:"$(back)"}
execute if score #rs.dust ra.temp matches 1.. run return 1

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/directional_sources[facing=$(back)] run return 1

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_omni run return 1

$return run function ra_lib:redstone/torch_present/$(torch) {dx:$(dx),dy:$(dy),dz:$(dz),side:"$(side)"}
