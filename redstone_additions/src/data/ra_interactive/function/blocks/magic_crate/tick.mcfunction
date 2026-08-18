# /ra_interactive:blocks/magic_crate/tick
# Tick all Magic Crates. Called once per game tick from ra_interactive:tick.

# Break detection.
execute as @e[type=marker,tag=ra.custom_block.magic_crate] at @s unless block ~ ~ ~ barrel run tag @s add ra.broken
kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run function ra_interactive:blocks/magic_crate/drop_all
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:barrel","minecraft:item_name":'Magic Crate',"minecraft:custom_data":{ra:{magic_crate:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.magic_crate"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] remove ra.broken

execute as @e[type=marker,tag=ra.custom_block.magic_crate] at @s run function ra_interactive:blocks/magic_crate/tick_one
