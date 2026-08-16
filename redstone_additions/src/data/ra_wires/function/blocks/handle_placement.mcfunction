# /ra_wires:blocks/handle_placement
# Place any RA Wires block.
# Context: as bat, at bat position.
#
# This used to be sixteen blocks of nine near-identical commands, each repeating
# the same marker selector eight times. Every new block meant another copy, and a
# fix to the placement sequence had to be applied sixteen times. The per-block
# part is now just the spec; blocks/place_generic does the work.
#
# Spec fields:
#   block     world block to place
#   marker    marker tag suffix, i.e. ra.custom_block.{marker}
#   fluid     1b to tag it as a fluid block for status and displays
#   net       transport network class to join, or absent for a block that is
#             not a network node (the Boiler sits between two networks, so it
#             must not be part of either)
#   capacity  this node's contribution to its network's capacity
#   electric  1b for the electric system, which is not on the engine
#   dir       placement dir_type: 0 none, 1 horizontal, 2 full 6-way (default 0)
#   props     data.properties for the new marker. Only list what the block
#             actually reads: a property nobody consults still shows up in the
#             Data Handler with a [Toggle] button that changes nothing visible.
#             Pipes and tanks are pure conductors and capacity, so they carry no
#             `enabled` -- use a valve to cut a line.

data remove storage ra:wires spec

execute if entity @s[tag=ra.place.liquid_pipe_copper] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:200,props:{tier:"copper"}}
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:260,props:{tier:"iron"}}
execute if entity @s[tag=ra.place.gas_pipe_copper] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:200,props:{tier:"copper"}}
execute if entity @s[tag=ra.place.gas_pipe_netherite] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"liquid_pipe",fluid:1b,net:"fluid",capacity:260,props:{tier:"iron"}}

execute if entity @s[tag=ra.place.liquid_tank] run data modify storage ra:wires spec set value {block:"minecraft:waxed_copper_block",marker:"liquid_tank",fluid:1b,net:"fluid",capacity:4000}
execute if entity @s[tag=ra.place.gas_tank] run data modify storage ra:wires spec set value {block:"minecraft:iron_block",marker:"gas_tank",fluid:1b,net:"fluid",capacity:3000}

execute if entity @s[tag=ra.place.liquid_pump] run data modify storage ra:wires spec set value {block:"minecraft:dispenser",marker:"liquid_pump",dir:2,fluid:1b,net:"fluid",capacity:500,props:{enabled:1b}}
execute if entity @s[tag=ra.place.gas_pump] run data modify storage ra:wires spec set value {block:"minecraft:smoker",marker:"gas_pump",dir:1,fluid:1b,net:"fluid",capacity:500,props:{enabled:1b}}

execute if entity @s[tag=ra.place.liquid_valve] run data modify storage ra:wires spec set value {block:"minecraft:waxed_cut_copper",marker:"liquid_valve",fluid:1b,net:"fluid",capacity:300,props:{enabled:1b}}
execute if entity @s[tag=ra.place.gas_valve] run data modify storage ra:wires spec set value {block:"minecraft:smooth_basalt",marker:"gas_valve",fluid:1b,net:"fluid",capacity:280,props:{enabled:1b}}

execute if entity @s[tag=ra.place.liquid_drain] run data modify storage ra:wires spec set value {block:"minecraft:dropper",marker:"liquid_drain",dir:2,fluid:1b,net:"fluid",capacity:600,props:{enabled:1b,mode:"drain"}}

execute if entity @s[tag=ra.place.electric_wire_copper] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"electric_wire",electric:1b,props:{tier:"copper",enabled:1b,transfer_rate:60},nodedata:{eu:0,capacity:220}}
execute if entity @s[tag=ra.place.electric_wire_netherite] run data modify storage ra:wires spec set value {block:"minecraft:conduit[waterlogged=false]",marker:"electric_wire",electric:1b,props:{tier:"netherite",enabled:1b,transfer_rate:95},nodedata:{eu:0,capacity:340}}
execute if entity @s[tag=ra.place.electric_generator] run data modify storage ra:wires spec set value {block:"minecraft:blast_furnace",marker:"electric_generator",electric:1b,props:{enabled:1b},nodedata:{eu:0,capacity:600}}
execute if entity @s[tag=ra.place.electric_consumer] run data modify storage ra:wires spec set value {block:"minecraft:observer",marker:"electric_consumer",electric:1b,props:{enabled:1b},nodedata:{eu:0,capacity:400}}
execute if entity @s[tag=ra.place.solar_panel] run data modify storage ra:wires spec set value {block:"minecraft:daylight_detector",marker:"solar_panel",electric:1b,props:{enabled:1b},nodedata:{eu:0,capacity:500}}
execute if entity @s[tag=ra.place.electric_switch] run data modify storage ra:wires spec set value {block:"minecraft:redstone_lamp",marker:"electric_switch",electric:1b,props:{enabled:1b},nodedata:{eu:0,capacity:200}}

execute if entity @s[tag=ra.place.boiler] run data modify storage ra:wires spec set value {block:"minecraft:furnace",marker:"boiler",props:{enabled:1b}}

execute unless data storage ra:wires spec run return 0

# Default the facing before the call: macro arguments are bound when the function
# is invoked, so a default written inside place_generic would arrive too late for
# its own $(dir). A dropper or dispenser without this always points the same way
# regardless of how the player placed it.
execute unless data storage ra:wires spec.dir run data modify storage ra:wires spec.dir set value 0

function ra_wires:blocks/place_generic with storage ra:wires spec
data remove storage ra:wires spec
return 1
