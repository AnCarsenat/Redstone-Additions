# /ra_ender:blocks/power_vault/tick
# Tick all Ender Power Vaults.

execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] at @s unless block ~ ~ ~ purpur_pillar run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_power_vault] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:purpur_pillar"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_power_vault] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:purpur_pillar","minecraft:item_name":"Ender Power Vault","minecraft:rarity":"rare","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Shares EU with the vault on its channel",color:"gray",italic:false},{text:"Behaves as an electric node: wire into it",color:"gray",italic:false},{text:"Shift+RMB with the wrench: link / send / receive",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{ender_power_vault:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.ender_power_vault"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_power_vault] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_power_vault] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_power_vault] remove ra.broken

# Defaults, including the electric-node tag a vault placed by an older build lacks.
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value "default"
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "link"
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 80
tag @e[type=marker,tag=ra.custom_block.ender_power_vault] add ra.wires.electric_node

tag @e[type=marker,tag=ra.ender.recv_power] remove ra.ender.recv_power

scoreboard players add @e[type=marker,tag=ra.custom_block.ender_power_vault] ra.ender.cd 1
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault,scores={ra.ender.cd=5..}] at @s run function ra_ender:blocks/power_vault/process
