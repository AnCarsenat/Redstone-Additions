# /ra_wires:debug/electric_one
# One node's report. Context: as its marker, at the block.

data modify storage ra:wires dbg set value {}
data modify storage ra:wires dbg.pos set from entity @s Pos
data modify storage ra:wires dbg.props set from entity @s data.properties
data modify storage ra:wires dbg.data set from entity @s data.data

# How many electric neighbours the transfer probe can reach. Same offsets and the
# same 0.75 radius transfer_adjacent uses, so a mismatch here is a mismatch there.
scoreboard players set #dbg.n ra.wires.tmp 0
execute positioned ~1 ~ ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1
execute positioned ~-1 ~ ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1
execute positioned ~ ~ ~1 if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1
execute positioned ~ ~ ~-1 if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1
execute positioned ~ ~1 ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1
execute positioned ~ ~-1 ~ if entity @e[type=marker,tag=ra.wires.electric_node,distance=..0.75,limit=1] run scoreboard players add #dbg.n ra.wires.tmp 1

tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"},{text:"  neighbours ",color:"gray"},{score:{name:"#dbg.n",objective:"ra.wires.tmp"},color:"yellow"}]
tellraw @s [{text:"   props ",color:"gray"},{nbt:"dbg.props",storage:"ra:wires",color:"white"}]
tellraw @s [{text:"   data ",color:"gray"},{nbt:"dbg.data",storage:"ra:wires",color:"white"}]

# The enabled flag has to be a byte for the old gates to match. An int reads the
# same in chat and behaves the opposite way.
execute if data entity @s data.properties{enabled:1b} run tellraw @s [{text:"   enabled: ",color:"gray"},{text:"1b, a byte",color:"green"}]
execute if data entity @s data.properties{enabled:0b} run tellraw @s [{text:"   enabled: ",color:"gray"},{text:"0b, switched off",color:"red"}]
execute unless data entity @s data.properties{enabled:1b} unless data entity @s data.properties{enabled:0b} run tellraw @s [{text:"   enabled: ",color:"gray"},{text:"not a byte — an int or a string",color:"red"}]

execute if entity @s[tag=ra.wires.did_move] run tellraw @s [{text:"   did_move still set — it should be cleared every tick",color:"red"}]
