# /ra_storage:blocks/unboxer/refresh_display
# Rebuild the Unboxer's dispenser skin.
# Context: at the Unboxer's block position.
#
# The block underneath is a barrel, because the Unboxer holds the crates it is
# unboxing in its own inventory and a vanilla dispenser fires its own contents on
# any rising redstone edge. The dispenser appearance is kept as a block_display
# laid over the barrel, so the block still reads as an Unboxer without carrying
# the behaviour that threw crates on the floor.
#
# The facing is read back off the barrel rather than from the marker, so a barrel
# rotated by any means still gets a skin pointing the same way.

kill @e[type=block_display,tag=ra.custom_block.display.unboxer,distance=..0.9]

execute unless block ~ ~ ~ minecraft:barrel run return 0

execute if block ~ ~ ~ minecraft:barrel[facing=north] run function ra_storage:blocks/unboxer/spawn_display {facing:"north"}
execute if block ~ ~ ~ minecraft:barrel[facing=south] run function ra_storage:blocks/unboxer/spawn_display {facing:"south"}
execute if block ~ ~ ~ minecraft:barrel[facing=east] run function ra_storage:blocks/unboxer/spawn_display {facing:"east"}
execute if block ~ ~ ~ minecraft:barrel[facing=west] run function ra_storage:blocks/unboxer/spawn_display {facing:"west"}
execute if block ~ ~ ~ minecraft:barrel[facing=up] run function ra_storage:blocks/unboxer/spawn_display {facing:"up"}
execute if block ~ ~ ~ minecraft:barrel[facing=down] run function ra_storage:blocks/unboxer/spawn_display {facing:"down"}
