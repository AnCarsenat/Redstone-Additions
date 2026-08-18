# /ra_wires:bridge/balanced
# Internal: the two sides already hold the same amount. Context: as the bridge.

data modify entity @s data.status.bridge_state set value "level"
data modify entity @s data.status.moved set value 0
