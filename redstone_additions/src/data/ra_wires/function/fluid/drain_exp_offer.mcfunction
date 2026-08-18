# /ra_wires:fluid/drain_exp_offer {amount:N}
# Internal: put the millilitres on the network. Context: as the drain marker.

$return run function ra_lib:transport/net/offer {amount:$(amount),medium:"experience"}
