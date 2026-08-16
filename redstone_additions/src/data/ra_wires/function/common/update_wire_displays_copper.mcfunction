# /ra_wires:common/update_wire_displays_copper
# Render L1 electric wire conduit visuals.
#
# Each node draws only its own half of a connection: from the core out to the
# block boundary, never across into the neighbour. Both nodes of a pair used to
# draw a full-length bar spanning centre to centre, so every connected pair
# rendered two identical boxes in the same space and z-fought.
#
# Electric wires are thin -- a 0.26 core against the fluid pipes' 0.56 -- and use
# concrete rather than metal, matching their candle-coloured items. Previously an
# L1 wire and an L1 fluid pipe rendered as exactly the same copper block.

# Core
execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.center"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.13f,-0.13f,-0.13f],scale:[0.26f,0.26f,0.26f]}}

# Half-connectors, one per connected side
execute positioned ~1 ~ ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~-1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0.13f,-0.13f,-0.13f],scale:[0.37f,0.26f,0.26f]}}
execute positioned ~-1 ~ ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.5f,-0.13f,-0.13f],scale:[0.37f,0.26f,0.26f]}}
execute positioned ~ ~ ~1 if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~-1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.13f,-0.13f,0.13f],scale:[0.26f,0.26f,0.37f]}}
execute positioned ~ ~ ~-1 if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.13f,-0.13f,-0.5f],scale:[0.26f,0.26f,0.37f]}}
execute positioned ~ ~1 ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~-1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.13f,0.13f,-0.13f],scale:[0.26f,0.37f,0.26f]}}
execute positioned ~ ~-1 ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.wire_display","ra.wires.wire_display.link"],block_state:{Name:"minecraft:orange_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.13f,-0.5f,-0.13f],scale:[0.26f,0.37f,0.26f]}}
