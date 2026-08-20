# /ra_interactive:blocks/big_torch/tick
# Tick all Big Torches. Called once per game tick from ra_interactive:tick.

# Break detection.
execute as @e[type=marker,tag=ra.custom_block.big_torch] at @s unless block ~ ~ ~ shroomlight run tag @s add ra.broken
kill @e[type=item,nbt={Item:{id:"minecraft:shroomlight"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.big_torch] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:torch","minecraft:item_name":'Big Torch',"minecraft:lore":[{text:"Stops hostile mobs spawning nearby",color:"gray",italic:false},{text:"Set the radius with the Data Handler",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{big_torch:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.big_torch"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.big_torch] at @s run playsound minecraft:block.stone.break block @a[distance=..16,scores={ra.u.snd=1..}] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.big_torch] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.big_torch] remove ra.broken

# THE SWEEP RUNS EVERY 10 TICKS, NOT EVERY TICK
# Its selector reaches as far as the radius does, which is up to 100 blocks, and
# there is no reason for it to be prompt: a mob that lives half a second before
# being denied is indistinguishable from one that never spawned. Paying a
# 100-block entity selector per torch per tick to shorten that would be the most
# expensive thing in the pack.
scoreboard players add #big_torch.scan ra.temp 1
execute unless score #big_torch.scan ra.temp matches 10.. run return 0
scoreboard players set #big_torch.scan ra.temp 0

execute as @e[type=marker,tag=ra.custom_block.big_torch] at @s run function ra_interactive:blocks/big_torch/sweep
