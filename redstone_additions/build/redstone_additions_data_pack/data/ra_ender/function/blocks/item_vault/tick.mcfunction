# /ra_ender:blocks/item_vault/tick
# Tick all Ender Item Vaults.

# Break detection.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] at @s unless block ~ ~ ~ barrel run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:barrel","minecraft:item_name":"Ender Item Vault","minecraft:rarity":"rare","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Shares its contents with the vault on its channel",color:"gray",italic:false},{text:"Shift+RMB with the wrench: link / send / receive",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{ender_item_vault:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.ender_item_vault"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.ender_item_vault] remove ra.broken

# Defaults for vaults placed before a property existed.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties.channel run data modify entity @s data.properties.channel set value "default"
# Migration: `shared` is gone. A vault still set to it becomes `link`, which is
# the closest of the three that remain — two-way and demand driven.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] if data entity @s data.properties{mode:"shared"} run data modify entity @s data.properties.mode set value "link"
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "link"

# Who can receive this tick. Recomputed rather than stored, so editing the mode
# with the Data Handler takes effect immediately.
# The `ra.ender.occupied` and `ra.ender.share` bookkeeping went with `shared`:
# both existed only to work out which vault a player was standing at. Two
# whole-world sweeps a tick, gone.

tag @e[type=marker,tag=ra.ender.recv_item] remove ra.ender.recv_item
tag @e[type=marker,tag=ra.ender.send_item] remove ra.ender.send_item

# Who can receive this tick. Recomputed every tick rather than stored, so changing
# the mode with the wrench takes effect immediately.
#
# THESE WERE LOST IN v5.1.8, AND THE VAULTS HAVE NOT LINKED SINCE
# They used to carry an `unless data.properties{enabled:0b}` clause. When the
# `enabled` property was removed from the module, the whole line went with it
# instead of just that clause -- so the tags were still cleared every tick and
# still required by link/send_items, but nothing ever added them again. A sending
# vault searched for a receiver wearing a tag no vault could be wearing, found
# nothing, and silently did nothing.
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties{mode:"send"} run tag @s add ra.ender.recv_item
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] unless data entity @s data.properties{mode:"receive"} run tag @s add ra.ender.send_item

# Hopper rate: one stack every 4 ticks per sending vault.
scoreboard players add @e[type=marker,tag=ra.custom_block.ender_item_vault] ra.ender.cd 1
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault,scores={ra.ender.cd=4..}] at @s run function ra_ender:blocks/item_vault/process
