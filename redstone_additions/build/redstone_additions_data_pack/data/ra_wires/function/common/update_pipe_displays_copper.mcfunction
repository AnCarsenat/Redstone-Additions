# /ra_wires:common/update_pipe_displays_copper
# Render L1 (copper tier) fluid pipe conduit visuals.
#
# Each node draws only its own half of a connection: from the core out to the
# block boundary, never across into the neighbour. Both nodes of a pair used to
# draw a full-length bar spanning centre to centre, so every connected pair
# rendered two identical boxes in the same space and z-fought.
#
# Fluid pipes are the chunky ones: a 0.56 core in copper or iron. Electric wires
# are deliberately half that thickness and a different palette, because an L1
# pipe and an L1 wire were both a 0.56 copper_block and indistinguishable.

# Core
execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.center"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,-0.28f],scale:[0.56f,0.56f,0.56f]}}

# Half-connectors, one per connected side
execute positioned ~1 ~ ~ if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~-1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0.28f,-0.28f,-0.28f],scale:[0.22f,0.56f,0.56f]}}
execute positioned ~-1 ~ ~ if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.5f,-0.28f,-0.28f],scale:[0.22f,0.56f,0.56f]}}
execute positioned ~ ~ ~1 if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~-1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,0.28f],scale:[0.56f,0.56f,0.22f]}}
execute positioned ~ ~ ~-1 if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,-0.5f],scale:[0.56f,0.56f,0.22f]}}
execute positioned ~ ~1 ~ if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~-1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,0.28f,-0.28f],scale:[0.56f,0.22f,0.56f]}}
execute positioned ~ ~-1 ~ if entity @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1,sort=nearest] positioned ~ ~1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.5f,-0.28f],scale:[0.56f,0.22f,0.56f]}}
