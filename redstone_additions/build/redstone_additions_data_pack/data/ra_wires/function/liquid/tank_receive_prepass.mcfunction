# /ra_wires:liquid/tank_receive_prepass
# Give tanks a receive-priority pull before generic source push.
# Context: as liquid tank marker, at tank position

execute unless data entity @s data.properties{enabled:1b} run return 0
execute if data entity @s data.properties.pipe_io unless data entity @s data.properties.pipe_io{input:1b} run return 0

# +X
execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src

# -X
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src

# +Z
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src

# -Z
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src

# +Y
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src

# -Y
execute positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.pipe_io_endpoint,tag=!ra.wires.did_move,distance=..0.99,limit=1,sort=nearest] run tag @s add ra.wires.liq_src
function ra_wires:liquid/try_receive_from_source
tag @e[type=marker,tag=ra.wires.liq_src,distance=..1.1] remove ra.wires.liq_src