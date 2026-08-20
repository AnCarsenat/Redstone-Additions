# /ra_wires:fluid/drain_exp_pull {amount:N}
# Internal: take the millilitres back off the network. Context: as the drain.

$return run function ra_lib:transport/net/take {amount:$(amount),medium:"experience"}
