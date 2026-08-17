# /ra_wires:fluid/refresh_status
# Copy this node's network state onto the marker for the goggles to read.
# Context: as a fluid node marker.

function ra_wires:media/label

execute store result entity @s data.status.amount int 1 run scoreboard players get #net_amount ra.tr.tmp
execute store result entity @s data.status.capacity int 1 run scoreboard players get #net_capacity ra.tr.tmp
execute store result entity @s data.status.network int 1 run scoreboard players get @s ra.tr.net
