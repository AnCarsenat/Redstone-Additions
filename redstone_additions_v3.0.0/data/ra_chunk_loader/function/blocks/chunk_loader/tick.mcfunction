# /ra_chunk_loader:blocks/chunk_loader/tick
# Tick all chunk loader markers

# Break detection
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.new] at @s unless block ~ ~ ~ minecraft:lodestone run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run function ra_chunk_loader:blocks/chunk_loader/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] remove ra.broken

# Redstone power detection
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.new] at @s run function ra_lib:redstone/detect

# Load chunk on rising edge
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.was_powered] if score @s ra.power matches 1.. at @s run function ra_chunk_loader:blocks/chunk_loader/on_powered

# Unload chunk on falling edge
execute as @e[tag=ra.custom_block.chunk_loader,tag=ra.was_powered] if score @s ra.power matches ..0 at @s run function ra_chunk_loader:blocks/chunk_loader/on_unpowered

# Track power state
tag @e[tag=ra.custom_block.chunk_loader,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.chunk_loader,tag=!ra.powered] remove ra.was_powered
