# /ra_interactive:blocks/item_mover/tick
# Tick all item movers

# Break detection â€” dispenser removed
execute as @e[tag=ra.custom_block.item_mover] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":"Item Mover","minecraft:lore":[{text:"Moves items from back to front",color:"gray",italic:false}],"minecraft:custom_data":{ra:{item_mover:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.item_mover"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.item_mover] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.item_mover] remove ra.broken

# Process each item mover
execute as @e[tag=ra.custom_block.item_mover] at @s run function ra_interactive:blocks/item_mover/process
