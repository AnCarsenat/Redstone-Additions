# data/ra_interactive/function/blocks/block_breaker/tick.mcfunction
# Tick all block breakers

# Check for break detection (block removed)
execute as @e[tag=ra.custom_block.block_breaker] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":'Block Breaker',"minecraft:custom_data":{ra:{block_breaker:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.block_breaker"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.block_breaker] remove ra.broken

# Process cooldowns
scoreboard players add @e[tag=ra.custom_block.block_breaker] ra.cooldown 1

# Check for powered dispensers (only if cooldown ready)
execute as @e[tag=ra.custom_block.block_breaker,scores={ra.cooldown=40..}] at @s if block ~ ~ ~ dispenser[triggered=true] run execute unless block ^ ^ ^1 #air run execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_lib:unbreakable run fill ~ ~ ~ ~ ~ ~ air destroy
execute as @e[tag=ra.custom_block.block_breaker,scores={ra.cooldown=40..}] at @s if block ~ ~ ~ dispenser[triggered=true] run execute unless block ^ ^ ^1 #air run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 0.8
execute as @e[tag=ra.custom_block.block_breaker,scores={ra.cooldown=40..}] at @s if block ~ ~ ~ dispenser[triggered=true] run execute unless block ^ ^ ^1 #air run execute positioned ^ ^ ^1 run particle minecraft:block{block_state:"minecraft:stone"} ~ ~ ~ 0.3 0.3 0.3 0.1 20
execute as @e[tag=ra.custom_block.block_breaker,scores={ra.cooldown=40..}] at @s if block ~ ~ ~ dispenser[triggered=true] run execute unless block ^ ^ ^1 #air run scoreboard players set @s ra.cooldown 0
