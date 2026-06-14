# /ra_wires:common/update_pipe_displays_copper
# Render L1 (copper tier) conduit visuals.

# Center visual
execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.center"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,-0.28f],scale:[0.56f,0.56f,0.56f]}}

# East / West bridge bars
execute positioned ~1 ~ ~ if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~-1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,-0.28f,-0.28f],scale:[1.0f,0.56f,0.56f]}}
execute positioned ~-1 ~ ~ if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~1 ~ ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-1.0f,-0.28f,-0.28f],scale:[1.0f,0.56f,0.56f]}}

# South / North bridge bars
execute positioned ~ ~ ~1 if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~-1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,0f],scale:[0.56f,0.56f,1.0f]}}
execute positioned ~ ~ ~-1 if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~ ~ ~1 align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-0.28f,-1.0f],scale:[0.56f,0.56f,1.0f]}}

# Up / Down bridge bars
execute positioned ~ ~1 ~ if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~ ~-1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,0f,-0.28f],scale:[0.56f,1.0f,0.56f]}}
execute positioned ~ ~-1 ~ if entity @e[type=marker,tag=ra.wires.pipe_io_endpoint,distance=..0.75,limit=1,sort=nearest] positioned ~ ~1 ~ align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:copper_block"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.28f,-1.0f,-0.28f],scale:[0.56f,1.0f,0.56f]}}
