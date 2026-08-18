# /ra_interactive:blocks/block_placer/tick
# Tick all block placers

# Check for break detection
execute as @e[type=marker,tag=ra.custom_block.block_placer] at @s unless block ~ ~ ~ dispenser run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dispenser"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute if data block ~ ~ ~ Items[0] run summon item ~ ~0.5 ~ {Tags:["ra.drop_temp"]}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute if entity @e[type=item,tag=ra.drop_temp,limit=1] run data modify entity @e[type=item,tag=ra.drop_temp,limit=1] Item set from block ~ ~ ~ Items[0]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run execute as @e[type=item,tag=ra.drop_temp] run tag @s remove ra.drop_temp
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":"Block Placer","minecraft:custom_data":{ra:{block_placer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.block_placer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.block_placer] remove ra.broken

# One placement per second while held. This used to fire on every tick a signal
# was present, which emptied a stocked placer in under two seconds and made it
# impossible to use with anything but a single pulse.
scoreboard players add @e[type=marker,tag=ra.custom_block.block_placer] ra.cooldown 1

# Check for powered dispensers

# One gated call instead of five identical selector sweeps. See fire.mcfunction
# for why the reset has to happen before the block is placed.
execute as @e[type=marker,tag=ra.custom_block.block_placer,scores={ra.cooldown=20..}] at @s if block ~ ~ ~ dispenser[triggered=true] if block ^ ^ ^1 air if data block ~ ~ ~ Items[0] run function ra_interactive:blocks/block_placer/fire
