# /ra_lib_multiblock:register
# Register one multiblock type. Call from a function listed in
# #ra_lib_multiblock:load, after writing the spec to storage ra:multiblock spec:
#
#   data modify storage ra:multiblock spec set value {
#     id:"rock_metallic_drill",
#     name:"Rock Metallic Drill",
#     blocks:[
#       {x:0,y:0,z:0,  match:"minecraft:waxed_copper_block"},
#       {x:0,y:0,z:-1, match:"minecraft:barrel"},
#       {x:0,y:1,z:0,  match:"#ra_multiblock:blast_forge_bricks"}
#     ],
#     inputs:[{name:"input_1",x:0,y:0,z:-1}],
#     outputs:[{name:"output_1",x:0,y:0,z:-1}],
#     controls:[{name:"redstone_in",x:0,y:0,z:1}]
#   }
#   function ra_lib_multiblock:register with storage ra:multiblock spec
#
# Offsets are written ONCE, relative to the base block, for a structure facing
# NORTH. The library rotates them into the other three facings itself — the four
# hand-maintained coordinate tables the older multiblocks carry are what made
# adding a structure so error-prone.
#
# `match` accepts a block id or a #block_tag; it is passed straight to
# `execute if block`, so block states work too ("minecraft:barrel[facing=up]").
#
# Set rotates:0b on a rotationally symmetric structure to have all four facings
# resolve to the authored offsets unchanged.

$data modify storage ra:multiblock types.$(id) set from storage ra:multiblock spec
$data modify storage ra:multiblock registry append value "$(id)"

data remove storage ra:multiblock spec
