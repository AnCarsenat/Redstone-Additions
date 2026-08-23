# /ra_wires:bridge/read_amount {medium:"water"}
# Internal: how much of one medium the network just read holds. Returns it.
# Context: anywhere, immediately after ra_lib:transport/net/read.

$execute store result score #br.amt ra.wires.tmp run data get storage ra:transport cur.amounts.$(medium)
return run scoreboard players get #br.amt ra.wires.tmp
