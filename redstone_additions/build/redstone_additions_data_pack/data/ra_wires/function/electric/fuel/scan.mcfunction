# /ra_wires:electric/fuel/scan
# Light the next piece of fuel in the generator's own inventory.
# Context: as the generator marker, at its block. Only called when nothing is
# currently burning.
#
# Matched against Items[0] rather than a container slot on purpose: Items is the
# list of stacks that are actually there, so Items[0] is the first occupied stack
# whatever slot it sits in — and it is exactly the stack
# ra_lib:inventory/consume_entry0 will take. Testing slot 0 instead would light a
# fuel and then burn whatever happened to be first.

execute unless data block ~ ~ ~ Items[0] run return 0

data modify storage ra:wires fq.queue set from storage ra:wires fuels
function ra_wires:electric/fuel/next
data remove storage ra:wires fq
