# /ra_wires:bridge/take {amount:N,medium:"..."}
# Internal: debit the network behind. Context: as the node behind.
# The amount was already clamped against both sides, so this cannot come up short.

$function ra_lib:transport/net/take {amount:$(amount)}
