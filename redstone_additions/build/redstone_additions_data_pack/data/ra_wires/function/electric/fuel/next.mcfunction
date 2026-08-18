# /ra_wires:electric/fuel/next
# Internal: walk the fuel registry until one matches what is in the generator.

execute if data entity @s data.data.burn run return 0
execute unless data storage ra:wires fq.queue[0] run return 0

data modify storage ra:wires fq.cur set from storage ra:wires fq.queue[0]
data remove storage ra:wires fq.queue[0]

function ra_wires:electric/fuel/try with storage ra:wires fq.cur
function ra_wires:electric/fuel/next
