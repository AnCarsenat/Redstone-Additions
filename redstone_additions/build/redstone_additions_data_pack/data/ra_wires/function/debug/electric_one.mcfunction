# /ra_wires:debug/electric_one
# One node's report. Context: as its marker, at the block.
#
# Rewritten for the network model. The questions worth asking changed with it:
# under the push model you wanted to know a node's buffer, its transfer rate and
# whether the adjacency probe could reach its neighbours. None of that exists any
# more. What matters now is which grid a node is on and what that grid holds —
# two nodes that look identical but report different network ids are not
# connected, and that is the failure this is for.

data modify storage ra:wires dbg set value {}
data modify storage ra:wires dbg.pos set from entity @s Pos
data modify storage ra:wires dbg.props set from entity @s data.properties

function ra_lib:transport/net/read

tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"},{text:"  grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  contributes ",color:"gray"},{score:{name:"@s",objective:"ra.tr.cap"},color:"yellow"}]
tellraw @s [{text:"   grid holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"yellow"}]
tellraw @s [{text:"   props ",color:"gray"},{nbt:"dbg.props",storage:"ra:wires",color:"white"}]

# A node with grid 0 is on no network at all: either a switch that has been
# turned off, or something that never joined.
execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid — switched off, or it never joined",color:"red"}]
execute unless entity @s[tag=ra.tr.node] run tellraw @s [{text:"   missing ra.tr.node — ra_wires:electric/adopt should have enrolled it",color:"red"}]

# The enabled flag has to be a byte. An int reads the same in chat and behaves
# the opposite way.

# Left over from the push model; nothing writes it now, so seeing one means an
# old marker was never adopted.
execute if data entity @s data.data.eu run tellraw @s [{text:"   stale per-node eu buffer — adopt did not run on this marker",color:"red"}]
