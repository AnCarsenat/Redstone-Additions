# /ra_ender:blocks/fluid_vault/tick
# Tick all Ender Fluid Vaults.

# Break detection. The node has to leave its network before the marker dies, or
# the network's contents go with it.
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] at @s unless block ~ ~ ~ purpur_block run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] at @s run function ra_lib:transport/net/leave
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:purpur_block"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:purpur_block","minecraft:item_name":"Ender Fluid Vault","minecraft:rarity":"rare","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Shares liquid and gas with the vault on its channel",color:"gray",italic:false},{text:"Joins the fluid network like a tank",color:"gray",italic:false},{text:"Shift+RMB with the wrench: link / send / receive",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{ender_fluid_vault:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.ender_fluid_vault"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_fluid_vault] remove ra.broken

# Defaults, and a node that lost its network membership on reload rejoins.
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value "default"
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "link"
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless data entity @s data.properties.transfer_rate run data modify entity @s data.properties.transfer_rate set value 200
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault,tag=!ra.tr.node] run function ra_lib:transport/net/rejoin {class:"fluid"}

tag @e[type=marker,tag=ra.ender.recv_fluid] remove ra.ender.recv_fluid
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless data entity @s data.properties{enabled:0b} unless data entity @s data.properties{mode:"send"} run tag @s add ra.ender.recv_fluid

# Every 10 ticks, so a link is a steady trickle rather than a spike.
scoreboard players add @e[type=marker,tag=ra.custom_block.ender_fluid_vault] ra.ender.cd 1
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault,scores={ra.ender.cd=10..}] at @s run function ra_ender:blocks/fluid_vault/process
