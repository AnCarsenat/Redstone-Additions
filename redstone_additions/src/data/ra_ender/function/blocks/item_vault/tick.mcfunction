# /ra_ender:blocks/item_vault/tick
# Tick all Ender Item Vaults.

# Break detection.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] at @s unless block ~ ~ ~ barrel run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:barrel","minecraft:item_name":"Ender Item Vault","minecraft:rarity":"rare","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Shares its contents with the vault on its channel",color:"gray",italic:false},{text:"Shift+RMB with the wrench: link / send / receive",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{ender_item_vault:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.ender_item_vault"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] remove ra.broken

# Defaults for vaults placed before a property existed.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value "default"
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "shared"
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b

# Who can receive this tick. Recomputed rather than stored, so editing the mode
# with the Data Handler takes effect immediately.
# Which vaults are being stood at, and which are in shared mode. Both are
# recomputed rather than stored, so a mode change takes effect at once.
tag @e[type=marker,tag=ra.ender.occupied] remove ra.ender.occupied
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] at @s if entity @p[distance=..4] run tag @s add ra.ender.occupied
tag @e[type=marker,tag=ra.ender.share] remove ra.ender.share
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] if data entity @s data.properties{mode:"shared"} unless data entity @s data.properties{enabled:0b} run tag @s add ra.ender.share

tag @e[type=marker,tag=ra.ender.recv_item] remove ra.ender.recv_item
tag @e[type=marker,tag=ra.ender.send_item] remove ra.ender.send_item
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties{enabled:0b} unless data entity @s data.properties{mode:"send"} unless data entity @s data.properties{mode:"shared"} run tag @s add ra.ender.recv_item
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties{enabled:0b} unless data entity @s data.properties{mode:"receive"} unless data entity @s data.properties{mode:"shared"} run tag @s add ra.ender.send_item

# Hopper rate: one stack every 4 ticks per sending vault.
scoreboard players add @e[type=marker,tag=ra.custom_block.ender_item_vault] ra.ender.cd 1
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault,scores={ra.ender.cd=4..}] at @s run function ra_ender:blocks/item_vault/process
