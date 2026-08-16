# /ra_wires:media/state_is {medium:"...",state:"liquid"}
# Returns 1 when the medium is registered with that state.
# Used to keep gas out of liquid tanks and the other way round.

$execute if data storage ra:wires media.$(medium){state:"$(state)"} run return 1
return 0
