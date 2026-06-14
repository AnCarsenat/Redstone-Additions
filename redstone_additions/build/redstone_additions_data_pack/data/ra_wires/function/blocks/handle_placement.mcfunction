# /ra_wires:blocks/handle_placement
# Placement router for all RA Wires blocks

# ------------------------------ FLUID (MERGED LIQUID + GAS) ------------------------------

# L1 copper pipe
execute if entity @s[tag=ra.place.liquid_pipe_copper] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"liquid_pipe",dir_type:0}
execute if entity @s[tag=ra.place.liquid_pipe_copper] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"copper",enabled:1b,transfer_rate:50},data:{medium_id:0,amount:0,capacity:200},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.liquid_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_pipe_copper] as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_pipe_copper] run return 1

# L2 iron pipe
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"liquid_pipe",dir_type:0}
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"iron",enabled:1b,transfer_rate:80},data:{medium_id:0,amount:0,capacity:260},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_pipe_netherite] as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_pipe_netherite] run return 1

# Legacy copper gas pipe placement now maps to merged fluid pipe
execute if entity @s[tag=ra.place.gas_pipe_copper] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"liquid_pipe",dir_type:0}
execute if entity @s[tag=ra.place.gas_pipe_copper] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"copper",enabled:1b,transfer_rate:50},data:{medium_id:0,amount:0,capacity:200},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.gas_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.gas_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.gas_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.gas_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.gas_pipe_copper] as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.gas_pipe_copper] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.gas_pipe_copper] run return 1

# Legacy L2 gas pipe placement now maps to merged fluid pipe
execute if entity @s[tag=ra.place.gas_pipe_netherite] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"liquid_pipe",dir_type:0}
execute if entity @s[tag=ra.place.gas_pipe_netherite] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"iron",enabled:1b,transfer_rate:80},data:{medium_id:0,amount:0,capacity:260},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.gas_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.gas_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.gas_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.gas_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.gas_pipe_netherite] as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.gas_pipe_netherite] run tag @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.gas_pipe_netherite] run return 1

# Liquid tank
execute if entity @s[tag=ra.place.liquid_tank] run function ra_lib:placement/place {block_id:"minecraft:waxed_copper_block",block_tag:"liquid_tank",dir_type:0}
execute if entity @s[tag=ra.place.liquid_tank] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:80},data:{medium_id:0,amount:0,capacity:4000},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.liquid_tank] run tag @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_tank] run tag @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_tank] run tag @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_tank] run tag @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_tank] as @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_tank] run tag @e[type=marker,tag=ra.custom_block.liquid_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_tank] run return 1

# Liquid pump
execute if entity @s[tag=ra.place.liquid_pump] run function ra_lib:placement/place {block_id:"minecraft:dispenser",block_tag:"liquid_pump",dir_type:2}
execute if entity @s[tag=ra.place.liquid_pump] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:100,generation_rate:80,medium_id:1},data:{medium_id:0,amount:0,capacity:500},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.liquid_pump] run tag @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_pump] run tag @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_pump] run tag @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_pump] run tag @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_pump] as @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_pump] run tag @e[type=marker,tag=ra.custom_block.liquid_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_pump] run return 1

# Liquid valve
execute if entity @s[tag=ra.place.liquid_valve] run function ra_lib:placement/place {block_id:"minecraft:waxed_cut_copper",block_tag:"liquid_valve",dir_type:0}
execute if entity @s[tag=ra.place.liquid_valve] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:50},data:{medium_id:0,amount:0,capacity:300},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.liquid_valve] run tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_valve] run tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_valve] run tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_valve] run tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_valve] as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_valve] run tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_valve] run return 1

# Liquid drain
execute if entity @s[tag=ra.place.liquid_drain] run function ra_lib:placement/place {block_id:"minecraft:dropper",block_tag:"liquid_drain",dir_type:2}
execute if entity @s[tag=ra.place.liquid_drain] run data merge entity @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:100,medium_id:1},data:{medium_id:0,amount:0,capacity:600},status:{medium:"Empty",drain_state:"idle"}}}
execute if entity @s[tag=ra.place.liquid_drain] run tag @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.liquid_drain] run tag @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.liquid_drain] run tag @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.liquid_drain] run tag @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.liquid_drain] as @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.liquid_drain] run tag @e[type=marker,tag=ra.custom_block.liquid_drain,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.liquid_drain] run return 1

# Gas tank
execute if entity @s[tag=ra.place.gas_tank] run function ra_lib:placement/place {block_id:"minecraft:iron_block",block_tag:"gas_tank",dir_type:0}
execute if entity @s[tag=ra.place.gas_tank] run data merge entity @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:70},data:{medium_id:0,amount:0,capacity:3000},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.gas_tank] run tag @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.gas_tank] run tag @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.gas_tank] run tag @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.gas_tank] run tag @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.gas_tank] as @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.gas_tank] run tag @e[type=marker,tag=ra.custom_block.gas_tank,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.gas_tank] run return 1

# Gas pump
execute if entity @s[tag=ra.place.gas_pump] run function ra_lib:placement/place {block_id:"minecraft:smoker",block_tag:"gas_pump",dir_type:1}
execute if entity @s[tag=ra.place.gas_pump] run data merge entity @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:90,generation_rate:70,medium_id:11},data:{medium_id:0,amount:0,capacity:500},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.gas_pump] run tag @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.gas_pump] run tag @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.gas_pump] run tag @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.gas_pump] run tag @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.gas_pump] as @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.gas_pump] run tag @e[type=marker,tag=ra.custom_block.gas_pump,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.gas_pump] run return 1

# Gas valve
execute if entity @s[tag=ra.place.gas_valve] run function ra_lib:placement/place {block_id:"minecraft:smooth_basalt",block_tag:"gas_valve",dir_type:0}
execute if entity @s[tag=ra.place.gas_valve] run data merge entity @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:50},data:{medium_id:0,amount:0,capacity:280},status:{medium:"Empty"}}}
execute if entity @s[tag=ra.place.gas_valve] run tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.gas_valve] run tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.gas_valve] run tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.fluid_node
execute if entity @s[tag=ra.place.gas_valve] run tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.pipe_io_endpoint
execute if entity @s[tag=ra.place.gas_valve] as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.gas_valve] run tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.gas_valve] run return 1

# ------------------------------ ELECTRIC ------------------------------

# Copper electric wire
execute if entity @s[tag=ra.place.electric_wire_copper] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"electric_wire",dir_type:0}
execute if entity @s[tag=ra.place.electric_wire_copper] run data merge entity @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"copper",enabled:1b,transfer_rate:60},data:{eu:0,capacity:220},status:{active:0b}}}
execute if entity @s[tag=ra.place.electric_wire_copper] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.electric_wire_copper] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.electric_wire_copper] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.electric_node
execute if entity @s[tag=ra.place.electric_wire_copper] as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.electric_wire_copper] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.electric_wire_copper] run return 1

# Netherite electric wire
execute if entity @s[tag=ra.place.electric_wire_netherite] run function ra_lib:placement/place {block_id:"minecraft:conduit[waterlogged=false]",block_tag:"electric_wire",dir_type:0}
execute if entity @s[tag=ra.place.electric_wire_netherite] run data merge entity @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{tier:"netherite",enabled:1b,transfer_rate:95},data:{eu:0,capacity:340},status:{active:0b}}}
execute if entity @s[tag=ra.place.electric_wire_netherite] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.electric_wire_netherite] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.electric_wire_netherite] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.electric_node
execute if entity @s[tag=ra.place.electric_wire_netherite] as @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.electric_wire_netherite] run tag @e[type=marker,tag=ra.custom_block.electric_wire,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.electric_wire_netherite] run return 1

# EU generator
execute if entity @s[tag=ra.place.electric_generator] run function ra_lib:placement/place {block_id:"minecraft:blast_furnace",block_tag:"electric_generator",dir_type:1}
execute if entity @s[tag=ra.place.electric_generator] run data merge entity @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:120,generation_rate:60},data:{eu:0,capacity:700},status:{active:0b}}}
execute if entity @s[tag=ra.place.electric_generator] run tag @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.electric_generator] run tag @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.electric_generator] run tag @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.electric_node
execute if entity @s[tag=ra.place.electric_generator] as @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.electric_generator] run tag @e[type=marker,tag=ra.custom_block.electric_generator,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.electric_generator] run return 1

# EU consumer
execute if entity @s[tag=ra.place.electric_consumer] run function ra_lib:placement/place {block_id:"minecraft:observer",block_tag:"electric_consumer",dir_type:2}
execute if entity @s[tag=ra.place.electric_consumer] run data merge entity @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:70,eu_use:40},data:{eu:0,capacity:220},status:{active:0b}}}
execute if entity @s[tag=ra.place.electric_consumer] run tag @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.electric_consumer] run tag @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.electric_consumer] run tag @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.electric_node
execute if entity @s[tag=ra.place.electric_consumer] as @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.electric_consumer] run tag @e[type=marker,tag=ra.custom_block.electric_consumer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.electric_consumer] run return 1

# EU switch
execute if entity @s[tag=ra.place.electric_switch] run function ra_lib:placement/place {block_id:"minecraft:redstone_lamp",block_tag:"electric_switch",dir_type:0}
execute if entity @s[tag=ra.place.electric_switch] run data merge entity @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] {data:{properties:{enabled:1b,transfer_rate:120},data:{eu:0,capacity:240},status:{active:0b}}}
execute if entity @s[tag=ra.place.electric_switch] run tag @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.node
execute if entity @s[tag=ra.place.electric_switch] run tag @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.tinkerable
execute if entity @s[tag=ra.place.electric_switch] run tag @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] add ra.wires.electric_node
execute if entity @s[tag=ra.place.electric_switch] as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] at @s run function ra_wires:common/update_model_local_and_neighbors
execute if entity @s[tag=ra.place.electric_switch] run tag @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.new,distance=..0.5,sort=nearest,limit=1] remove ra.new
execute if entity @s[tag=ra.place.electric_switch] run return 1

return 0
