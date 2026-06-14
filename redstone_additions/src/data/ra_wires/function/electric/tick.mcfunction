# /ra_wires:electric/tick
# Tick electric wires, generator, consumer, and switch

# Migrate legacy wall-based electric wires to conduit visuals.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ mud_brick_wall run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ polished_blackstone_wall run setblock ~ ~ ~ conduit[waterlogged=false]

# Keep electric wire conduits non-waterlogged.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]

# Self-heal missing wire displays without forcing per-tick full rebuilds.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.wire_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors

# One-time refresh for display geometry updates.
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=!ra.wires.wire_display_v1] at @s if block ~ ~ ~ conduit run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=!ra.wires.wire_display_v1] at @s if block ~ ~ ~ conduit run tag @s add ra.wires.wire_display_v1

# Break detection
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s unless block ~ ~ ~ conduit run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s unless block ~ ~ ~ blast_furnace run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s unless block ~ ~ ~ observer run tag @s add ra.broken
execute as @e[type=marker,tag=ra.custom_block.electric_switch] at @s unless block ~ ~ ~ redstone_lamp run tag @s add ra.broken

execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run tag @s remove ra.wires.electric_node
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run kill @e[type=block_display,tag=ra.wires.wire_display,distance=..1.5]
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:conduit"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:mud_brick_wall"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:polished_blackstone_wall"}},distance=..2,limit=2]
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run execute if data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:black_candle","minecraft:item_name":'L2 Wire',"minecraft:custom_data":{ra:{electric_wire_netherite:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_wire_netherite"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run execute unless data entity @s data.properties{tier:"netherite"} run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:orange_candle","minecraft:item_name":'Copper Wire',"minecraft:custom_data":{ra:{electric_wire_copper:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_wire_copper"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run tag @s remove ra.wires.electric_node
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:blast_furnace"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blast_furnace","minecraft:item_name":'EU Generator',"minecraft:custom_data":{ra:{electric_generator:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_generator"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run tag @s remove ra.wires.electric_node
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:observer"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:observer","minecraft:item_name":'EU Consumer',"minecraft:custom_data":{ra:{electric_consumer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_consumer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.broken] at @s run kill @s
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run tag @s remove ra.wires.node
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run tag @s remove ra.wires.electric_node
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run function ra_wires:common/update_model_local_and_neighbors
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:redstone_lamp"}},distance=..2,limit=1]
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:redstone_lamp","minecraft:item_name":'EU Switch',"minecraft:custom_data":{ra:{electric_switch:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.electric_switch"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.broken] at @s run kill @s

tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_wire] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_generator] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_consumer] remove ra.broken
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.electric_switch] remove ra.broken

# Ensure every electric node has complete runtime data before processing.
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/init_node

# Generator production
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s run function ra_wires:electric/generator_tick

# Transfer through network
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/process_source

# Consumer draw
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s run function ra_wires:electric/consumer_tick

# Status refresh
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/update_status
