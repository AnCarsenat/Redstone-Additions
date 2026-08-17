# /ra_wires:fluid/source/probe_test {match,medium,volume,drained}
# Internal: record the entry if the block here matches it.

$execute unless block ~ ~ ~ $(match) run return 0

data modify storage ra:wires found set from storage ra:wires probe.cur
data modify storage ra:wires found.dx set from storage ra:wires probe.dx
data modify storage ra:wires found.dy set from storage ra:wires probe.dy
data modify storage ra:wires found.dz set from storage ra:wires probe.dz
