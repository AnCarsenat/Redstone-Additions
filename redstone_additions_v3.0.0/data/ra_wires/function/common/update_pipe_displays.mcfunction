# /ra_wires:common/update_pipe_displays
# Draw conduit connection visuals for fluid pipes using block displays
# Context: as fluid pipe marker, at pipe position

# Remove existing local displays around this pipe
kill @e[type=block_display,tag=ra.wires.pipe_display,distance=..0.75]

execute unless block ~ ~ ~ conduit run return 0

execute if data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_pipe_displays_netherite
execute unless data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_pipe_displays_copper