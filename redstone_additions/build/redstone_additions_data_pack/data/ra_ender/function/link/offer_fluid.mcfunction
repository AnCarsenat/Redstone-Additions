# /ra_ender:link/offer_fluid {amount:N,medium:"..."}
# Context: as the receiving node. Splits the macro so the amount can come from a
# score rather than being known when send_fluid was written.

$function ra_lib:transport/net/offer {amount:$(amount),medium:"$(medium)"}
