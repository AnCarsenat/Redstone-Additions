# /ra_interactive:blocks/pusher/tick
# Tick all pushers

# Check for break detection
execute as @e[tag=ra.custom_block.pusher] at @s if block ~ ~ ~ #minecraft:air run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.pusher] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:magenta_glazed_terracotta"}},distance=..2,limit=1,sort=nearest]
execute as @e[tag=ra.broken,tag=ra.custom_block.pusher] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:magenta_glazed_terracotta","minecraft:item_name":"Pusher","minecraft:custom_data":{ra:{pusher:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.pusher"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[tag=ra.broken,tag=ra.custom_block.pusher] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[tag=ra.broken,tag=ra.custom_block.pusher] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[tag=ra.broken,tag=ra.custom_block.pusher] at @s run kill @s
tag @e[tag=ra.broken,tag=ra.custom_block.pusher] remove ra.broken

# Initialize cooldown for new pushers (uninitialized scores don't match 0)
execute as @e[tag=ra.custom_block.pusher] unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0

# Decrement cooldown (only if cooldown is greater than 0)
execute as @e[tag=ra.custom_block.pusher,scores={ra.cooldown=1..}] if entity @e[type=!armor_stand,type=!item_frame,type=!marker,dx=0.5,dy=0.5,dz=0.5] run scoreboard players remove @s ra.cooldown 1

# Process each pusher - push entities above (only if cooldown is 0)
execute as @e[tag=ra.custom_block.pusher,scores={ra.cooldown=0}] at @s run function ra_interactive:blocks/pusher/process
