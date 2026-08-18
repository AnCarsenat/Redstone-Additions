# data/ra_interactive/function/blocks/block_breaker/tick.mcfunction
# Tick all block breakers

# Check for break detection (block removed)
execute as @e[type=marker,tag=ra.custom_block.block_breaker] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":'Block Breaker',"minecraft:custom_data":{ra:{block_breaker:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.block_breaker"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_breaker] remove ra.broken

# Process cooldowns
scoreboard players add @e[type=marker,tag=ra.custom_block.block_breaker] ra.cooldown 1

# Check for powered dispensers (only if cooldown ready)

# One gated call instead of five identical selector sweeps that each re-tested the
# same conditions -- and re-tested them AFTER the first had already changed the
# world. See fire.mcfunction.
execute as @e[type=marker,tag=ra.custom_block.block_breaker,scores={ra.cooldown=20..}] at @s if block ~ ~ ~ dispenser[triggered=true] unless block ^ ^ ^1 #air run function ra_interactive:blocks/block_breaker/fire
