# /ra_chunk_loader:blocks/chunk_loader/tick
# Tick all chunk loader markers

# Break detection
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.new] at @s unless block ~ ~ ~ minecraft:lodestone run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run execute at @s run forceload remove ~ ~
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:lodestone"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:lodestone","minecraft:item_name":"Chunk Loader","minecraft:lore":[{text:"Loads its chunk while powered",color:"gray",italic:false}],"minecraft:custom_data":{ra:{chunk_loader:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.chunk_loader"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.chunk_loader] remove ra.broken

# Redstone power detection
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.new] at @s run function ra_lib:redstone/detect

# Load chunk on rising edge
execute as @e[tag=ra.custom_block.chunk_loader,tag=!ra.was_powered] if score @s ra.power matches 1.. at @s run execute at @s run forceload add ~ ~

# Unload chunk on falling edge
execute as @e[tag=ra.custom_block.chunk_loader,tag=ra.was_powered] if score @s ra.power matches ..0 at @s run function ra_chunk_loader:blocks/chunk_loader/on_unpowered

# Track power state
tag @e[tag=ra.custom_block.chunk_loader,tag=ra.powered] add ra.was_powered
tag @e[tag=ra.custom_block.chunk_loader,tag=!ra.powered] remove ra.was_powered
