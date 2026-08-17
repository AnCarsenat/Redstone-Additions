# /ra_wires:tools/valve_apply
# Make a valve's enabled state actually cut the network.
# Context: as the valve marker.
#
# Rejoining keeps the capacity the valve was placed with. The previous version
# passed a flat 300 on the way back in, which quietly gave a gas valve (280) the
# liquid valve's capacity every time it was closed and reopened.

execute if data entity @s data.properties{enabled:1b} run function ra_lib:transport/net/rejoin {class:"fluid"}
execute unless data entity @s data.properties{enabled:1b} run function ra_lib:transport/net/leave
