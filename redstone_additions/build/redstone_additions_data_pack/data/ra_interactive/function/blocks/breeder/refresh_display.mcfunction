# /ra_interactive:blocks/breeder/refresh_display
# Rebuild the Breeder's dispenser skin.
# Context: at the breeder's block position.
#
# The block underneath is a barrel, because the breeder holds the food it feeds
# and a vanilla dispenser throws its own contents on any rising redstone edge.
# See ra_lib:skin/apply for the full reasoning.

function ra_lib:skin/apply {real:"minecraft:barrel",skin:"minecraft:dispenser",id:"breeder"}
