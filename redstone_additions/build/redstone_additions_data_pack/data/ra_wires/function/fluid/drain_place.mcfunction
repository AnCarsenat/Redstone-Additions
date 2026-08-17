# /ra_wires:fluid/drain_place
# Spend network contents placing the medium into the world.
# Context: as a drain marker in "place" mode, at the drain position.

function ra_lib:transport/net/read

execute unless data storage ra:transport cur.medium run data modify entity @s data.status.drain_state set value "network_empty"
execute unless data storage ra:transport cur.medium run return 0

execute if score #net_amount ra.tr.tmp matches ..999 run data modify entity @s data.status.drain_state set value "not_enough"
execute if score #net_amount ra.tr.tmp matches ..999 run return 0

function ra_wires:fluid/drain_place_run with storage ra:transport cur
