# /ra_wires:electric/peek_eu
# How much EU this node's network is holding, without taking any of it.
# Returns the stored amount, 0 when the node is on no network at all.
# Context: as an electric node marker.
#
# take_eu answers "could I have some" only by helping itself, which is the wrong
# question for anything that wants to know whether it COULD run -- a machine
# asking that every tick would drain the grid just by looking at it. net/read is
# already the non-destructive half of net/take, so this is only a name for it in
# EU terms.

function ra_lib:transport/net/read
return run scoreboard players get #net_amount ra.tr.tmp
