# /ra_chunk_loader:blocks/chunk_loader/on_break
# Called when chunk loader is broken
# Context: as armor stand, at position

# Ensure the chunk is no longer force-loaded when block is removed
execute at @s run forceload remove ~ ~

# Kill vanilla lodestone drop
kill @e[type=item,nbt={Item:{id:"minecraft:lodestone"}},distance=..2,limit=1,sort=nearest]

# Drop chunk loader item
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:lodestone","minecraft:item_name":"Chunk Loader","minecraft:lore":[{text:"Loads its chunk while powered",color:"gray",italic:false}],"minecraft:custom_data":{ra:{chunk_loader:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.chunk_loader"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}

playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5

kill @s
