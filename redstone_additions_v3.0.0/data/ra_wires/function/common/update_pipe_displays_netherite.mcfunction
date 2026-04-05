# /ra_wires:common/update_pipe_displays_netherite
# Render L2 (netherite tier) conduit visuals.

# Center visual
execute align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.center"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6125f,0.6125f,0.6125f]}}

# East / West bridge bars
execute if block ~1 ~ ~ conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0.5f,0f,0f],scale:[1.0f,0.6125f,0.6125f]}}
execute if block ~-1 ~ ~ conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.5f,0f,0f],scale:[1.0f,0.6125f,0.6125f]}}

# South / North bridge bars
execute if block ~ ~ ~1 conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0.5f],scale:[0.6125f,0.6125f,1.0f]}}
execute if block ~ ~ ~-1 conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,-0.5f],scale:[0.6125f,0.6125f,1.0f]}}

# Up / Down bridge bars
execute if block ~ ~1 ~ conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0.5f,0f],scale:[0.6125f,1.0f,0.6125f]}}
execute if block ~ ~-1 ~ conduit align xyz positioned ~0.5 ~0.5 ~0.5 positioned ~-.5 ~-.5 ~-.5 run summon block_display ~ ~ ~ {Tags:["ra.wires.pipe_display","ra.wires.pipe_display.link"],block_state:{Name:"minecraft:blue_concrete"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,-0.5f,0f],scale:[0.6125f,1.0f,0.6125f]}}
