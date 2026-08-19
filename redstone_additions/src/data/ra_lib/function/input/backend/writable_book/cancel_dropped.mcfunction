# /ra_lib:input/backend/writable_book/cancel_dropped
# The player threw the Input Form away. Treat that as "never mind" and tear the
# session down. Context: as the player.
#
# ra_lib:input/cancel runs session/cleanup, which for this backend kills the
# dropped item and clears any copy still in the inventory, so nothing is left
# lying on the floor.

tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Input cancelled.",color:"gray"}]
playsound minecraft:block.note_block.bass block @s ~ ~ ~ 0.5 0.8

function ra_lib:input/cancel
