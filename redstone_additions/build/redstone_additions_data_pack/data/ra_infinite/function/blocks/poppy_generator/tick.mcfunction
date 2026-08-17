# /ra_infinite:blocks/poppy_generator/tick
# Tick all Poppy Generators

# Break detection
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] at @s unless block ~ ~ ~ minecraft:dropper run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:dropper"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dropper","minecraft:item_name":"Poppy Generator","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Grows flowers in front of itself, forever",color:"gray",italic:false},{text:"Shift+RMB with the wrench: single / patch",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{poppy_generator:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.poppy_generator"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.poppy_generator] remove ra.broken

# Defaults for blocks placed before these properties existed.
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] unless data entity @s data.properties.cooldown run data modify entity @s data.properties.cooldown set value 80
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "single"

# One block, one marker. A placement that ran twice — a reload mid-place, or a
# duplicate bat — leaves two markers stacked on the same block, which ticks the
# generator twice and draws the goggles readout twice on top of itself.
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] at @s run kill @e[type=marker,tag=ra.custom_block.poppy_generator,distance=0.01..0.9,sort=nearest,limit=1]

scoreboard players add @e[type=marker,tag=ra.custom_block.poppy_generator] ra.cooldown 1
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] at @s run function ra_infinite:blocks/poppy_generator/process
