# /ra_wires:electric/offer_eu {amount:N}
# Internal: push EU into this node's network. Returns how much was accepted.
# Context: as an electric node marker.
#
# A macro wrapper because ra_lib:transport/net/offer takes its amount as a macro
# argument, and callers compute theirs in a scoreboard. Store the number into
# `storage ra:wires eu` and call this `with storage ra:wires eu`.

$return run function ra_lib:transport/net/offer {amount:$(amount),medium:"eu"}
