# /ra_interactive:blocks/breeder/tick
# Tick all breeders

# Check for break detection
execute as @e[tag=ra.custom_block.breeder] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.breeder] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.breeder] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name": 'Breeder',"minecraft:custom_data":{ra:{breeder:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.breeder"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.breeder] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.breeder] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.breeder] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.breeder] remove ra.broken

# Check for powered dispensers - activate breeding
execute as @e[tag=ra.custom_block.breeder] at @s if block ~ ~ ~ dispenser[triggered=true] run execute if data block ~ ~ ~ Items[0] positioned ^ ^ ^1 run function ra_interactive:blocks/breeder/try_breed
