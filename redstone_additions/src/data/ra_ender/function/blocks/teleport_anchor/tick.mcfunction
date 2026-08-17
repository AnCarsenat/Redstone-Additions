# /ra_ender:blocks/teleport_anchor/tick
# Tick all Teleport Anchors.

execute as @e[type=marker,tag=ra.custom_block.teleport_anchor] at @s unless block ~ ~ ~ crying_obsidian run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.teleport_anchor] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:crying_obsidian"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.teleport_anchor] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:crying_obsidian","minecraft:item_name":"Teleport Anchor","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Teleports whoever stands on it",color:"gray",italic:false},{text:"Redstone strength 1-15 picks the target id",color:"gray",italic:false},{text:"/function ra_ender:tools/anchor/help",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{teleport_anchor:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.teleport_anchor"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.teleport_anchor] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.teleport_anchor] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.teleport_anchor] remove ra.broken

# Defaults for anchors placed before a property existed.
execute as @e[type=marker,tag=ra.custom_block.teleport_anchor] unless data entity @s data.properties.anchor_id run data modify entity @s data.properties.anchor_id set value "A"
execute as @e[type=marker,tag=ra.custom_block.teleport_anchor] unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute as @e[type=marker,tag=ra.custom_block.teleport_anchor] unless data entity @s data.properties.targets run data modify entity @s data.properties.targets set value ["","","","","","","","","","","","","","",""]

# A selector on scores skips entities that have no score at all, so every anchor
# needs one before the cooldown gate can mean anything. Adding zero creates it.
scoreboard players add @e[type=marker,tag=ra.custom_block.teleport_anchor] ra.ender.tp_cd 0
scoreboard players remove @e[type=marker,tag=ra.custom_block.teleport_anchor,scores={ra.ender.tp_cd=1..}] ra.ender.tp_cd 1
execute as @e[type=marker,tag=ra.custom_block.teleport_anchor,scores={ra.ender.tp_cd=..0}] at @s run function ra_ender:blocks/teleport_anchor/process
