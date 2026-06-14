# /ra_chunk_loader:blocks/chunk_loader/on_unpowered
# Remove force-load from the chunk containing this marker
# Context: as chunk loader marker, at position

execute at @s run forceload remove ~ ~
