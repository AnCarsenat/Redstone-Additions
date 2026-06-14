# /ra_wires:tools/tinker_toggle_target
# Toggle enabled state, or cycle medium id for pumps/drains
# Context: as marker target

# Cycle medium on medium-producing/draining blocks
execute if entity @s[tag=ra.custom_block.liquid_pump] run execute store result score #mid ra.wires.tmp run data get entity @s data.properties.medium_id 1
execute if entity @s[tag=ra.custom_block.liquid_drain] run execute store result score #mid ra.wires.tmp run data get entity @s data.properties.medium_id 1
execute if entity @s[tag=ra.custom_block.gas_pump] run execute store result score #mid ra.wires.tmp run data get entity @s data.properties.medium_id 1

execute if entity @s[tag=ra.custom_block.liquid_pump] run scoreboard players add #mid ra.wires.tmp 1
execute if entity @s[tag=ra.custom_block.liquid_drain] run scoreboard players add #mid ra.wires.tmp 1
execute if entity @s[tag=ra.custom_block.gas_pump] run scoreboard players add #mid ra.wires.tmp 1
execute if entity @s[tag=ra.custom_block.liquid_pump] if score #mid ra.wires.tmp matches 6.. run scoreboard players set #mid ra.wires.tmp 1
execute if entity @s[tag=ra.custom_block.liquid_drain] if score #mid ra.wires.tmp matches 6.. run scoreboard players set #mid ra.wires.tmp 1
execute if entity @s[tag=ra.custom_block.gas_pump] if score #mid ra.wires.tmp matches ..10 run scoreboard players set #mid ra.wires.tmp 11
execute if entity @s[tag=ra.custom_block.gas_pump] if score #mid ra.wires.tmp matches 16.. run scoreboard players set #mid ra.wires.tmp 11

execute if entity @s[tag=ra.custom_block.liquid_pump] run execute store result entity @s data.properties.medium_id int 1 run scoreboard players get #mid ra.wires.tmp
execute if entity @s[tag=ra.custom_block.liquid_drain] run execute store result entity @s data.properties.medium_id int 1 run scoreboard players get #mid ra.wires.tmp
execute if entity @s[tag=ra.custom_block.gas_pump] run execute store result entity @s data.properties.medium_id int 1 run scoreboard players get #mid ra.wires.tmp

execute if entity @s[tag=ra.custom_block.liquid_pump] run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Pump medium id: ",color:"gray"},{nbt:"data.properties.medium_id",entity:"@s",color:"aqua"}]
execute if entity @s[tag=ra.custom_block.liquid_drain] run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Drain medium id: ",color:"gray"},{nbt:"data.properties.medium_id",entity:"@s",color:"aqua"}]
execute if entity @s[tag=ra.custom_block.gas_pump] run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Gas medium id: ",color:"gray"},{nbt:"data.properties.medium_id",entity:"@s",color:"aqua"}]

execute if entity @s[tag=ra.custom_block.liquid_pump] run return 1
execute if entity @s[tag=ra.custom_block.liquid_drain] run return 1
execute if entity @s[tag=ra.custom_block.gas_pump] run return 1

# Otherwise toggle enabled
execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute if data entity @s data.properties{enabled:1b} run tag @s add ra.wires.was_enabled
execute if entity @s[tag=ra.wires.was_enabled] run data modify entity @s data.properties.enabled set value 0b
execute unless entity @s[tag=ra.wires.was_enabled] run data modify entity @s data.properties.enabled set value 1b
tag @s remove ra.wires.was_enabled

execute if data entity @s data.properties{enabled:1b} run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Enabled",color:"green"}]
execute unless data entity @s data.properties{enabled:1b} run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Disabled",color:"red"}]

function ra_wires:common/update_model_local_and_neighbors
