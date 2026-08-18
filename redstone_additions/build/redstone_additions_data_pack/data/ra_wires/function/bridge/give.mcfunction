# /ra_wires:bridge/give {amount:N,medium:"..."}
# Internal: credit the network in front. Context: as the node in front.
# Carries the source's medium across, so the far side ends up holding what was
# actually sent rather than adopting a name of its own.

$function ra_lib:transport/net/offer {amount:$(amount),medium:"$(medium)"}
