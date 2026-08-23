# /ra_interactive:blocks/magic_crate/tick
# Tick all Magic Crates. Called once per game tick from ra_interactive:tick.

# Break detection.
execute as @e[type=marker,tag=ra.custom_block.magic_crate] at @s unless block ~ ~ ~ barrel run tag @s add ra.broken
# The vanilla drop goes, and the custom item is summoned in its place. This ran
# bare, so it killed whatever barrel happened to be within two blocks of wherever
# the tick function stood -- never the one that had just dropped out of this
# block. Breaking one handed back both items.
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run function ra_interactive:blocks/magic_crate/drop_all
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:barrel","minecraft:item_name":'Magic Crate',"minecraft:lore":[{text:"Pulls dropped items in from up to 20 blocks",color:"gray",italic:false}],"minecraft:custom_data":{ra:{magic_crate:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.magic_crate"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.magic_crate] remove ra.broken

execute as @e[type=marker,tag=ra.custom_block.magic_crate] at @s run function ra_interactive:blocks/magic_crate/tick_one
