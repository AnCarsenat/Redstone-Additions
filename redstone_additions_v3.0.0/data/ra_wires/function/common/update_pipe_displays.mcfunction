# /ra_wires:common/update_pipe_displays
# Draw conduit connection visuals for fluid pipes using block displays
# Context: as fluid pipe marker, at pipe position

# Remove existing local displays around this pipe.
# 0.9 removes stale offset displays from earlier builds while preserving neighbor centers at distance 1.0.
kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..0.9]

execute unless block ~ ~ ~ conduit run return 0

execute if data entity @s data.properties{tier:"iron"} run function ra_wires:common/update_pipe_displays_netherite
execute if data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_pipe_displays_netherite
execute unless data entity @s data.properties{tier:"iron"} unless data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_pipe_displays_copper