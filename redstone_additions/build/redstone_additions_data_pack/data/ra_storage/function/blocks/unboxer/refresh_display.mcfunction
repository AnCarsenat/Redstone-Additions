# /ra_storage:blocks/unboxer/refresh_display
# Rebuild the Unboxer's dispenser skin.
# Context: at the Unboxer's block position.
#
# The block underneath is a barrel, because the Unboxer holds the crates it is
# unboxing in its own inventory and a vanilla dispenser fires its own contents on
# any rising redstone edge. See ra_lib:skin/apply for the full reasoning.

function ra_lib:skin/apply {real:"minecraft:barrel",skin:"minecraft:dispenser",id:"unboxer"}
