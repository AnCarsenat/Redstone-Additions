# /ra_wires:electric/take_eu {amount:N}
# Internal: draw EU out of this node's network. Returns how much was actually
# removed, which is 0 when the grid is empty.
# Context: as an electric node marker.

$return run function ra_lib:transport/net/take {amount:$(amount)}
