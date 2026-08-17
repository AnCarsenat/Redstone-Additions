# /ra_wires:media/label
# Write a readable medium name into this node's status, from its network.
# Context: as a fluid node marker.

function ra_lib:transport/net/read

data modify entity @s data.status.medium set value "Empty"
execute if data storage ra:transport cur.medium run function ra_wires:media/label_run with storage ra:transport cur
