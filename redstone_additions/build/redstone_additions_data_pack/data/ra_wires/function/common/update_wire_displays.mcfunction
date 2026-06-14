# /ra_wires:common/update_wire_displays
# Draw conduit connection visuals for electric wires using block displays
# Context: as electric wire marker, at wire position

# Remove existing local displays around this wire.
kill @e[type=block_display,tag=ra.wires.wire_display,distance=..0.9]

execute unless block ~ ~ ~ conduit run return 0

execute if data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_wire_displays_netherite
execute unless data entity @s data.properties{tier:"netherite"} run function ra_wires:common/update_wire_displays_copper