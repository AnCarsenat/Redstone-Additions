# /ra_multiblock:register_types
# Register this module's structure-driven multiblocks.
# Hook: #ra_lib_multiblock:load
#
# Offsets are written once for a NORTH-facing structure, relative to the base
# block. ra_lib_multiblock:build derives south/east/west automatically, so there
# is no second (or third, or fourth) coordinate table to keep in step.
#
# Blast Forge and Upgrade Platform still use their own hand-written validators
# registered directly in the #ra_lib_multiblock tags. They work, so they were
# left alone; new structures should use this path.

# ============================================================================
# Rock Metallic Drill — copper tier
#
#   Side view, looking east, with the drill facing north (-Z):
#
#     B M     B = barrel   (input and output)   M = copper base
#     C S     C = iron bars (drill shaft)       S = smooth stone
#
# A redstone block on the control side switches the drill off.
# ============================================================================

data modify storage ra:multiblock spec set value {id:"rock_metallic_drill",name:"Rock Metallic Drill",tier:"copper",hint:"Need: barrel in front of the base, iron bars under the barrel, smooth stone under the base",blocks:[{x:0,y:0,z:0,match:"minecraft:waxed_copper_block"},{x:0,y:0,z:-1,match:"minecraft:barrel"},{x:0,y:-1,z:-1,match:"minecraft:iron_bars"},{x:0,y:-1,z:0,match:"minecraft:smooth_stone"}],inputs:[{name:"input_1",x:0,y:0,z:-1}],outputs:[{name:"output_1",x:0,y:0,z:-1}],controls:[{name:"redstone_in",x:0,y:0,z:1}]}
function ra_lib_multiblock:register with storage ra:multiblock spec
