# /ra_wires:debug/light_one
# One light's report. Context: as its marker, at the block.
#
# The beam needs four things at once and failing any of them looks the same from
# outside: enabled, redstone, EU it can actually take, and air to shine into.

data modify storage ra:wires dbg set value {}
data modify storage ra:wires dbg.pos set from entity @s Pos
tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"},{text:"  facing ",color:"gray"},{score:{name:"@s",objective:"ra.facing"},color:"aqua"}]


execute if function ra_lib:redstone/any run tellraw @s [{text:"   redstone: powered",color:"green"}]
execute unless function ra_lib:redstone/any run tellraw @s [{text:"   redstone: NONE — it needs a signal as well as EU",color:"red"}]

function ra_lib:transport/net/read
tellraw @s [{text:"   grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"}]
execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid — wire it to a battery",color:"red"}]
execute if score #net_amount ra.tr.tmp matches ..0 run tellraw @s [{text:"   grid is empty, so there is nothing to draw",color:"red"}]

execute unless score @s ra.facing matches 0..5 run tellraw @s [{text:"   no facing score — it does not know which way to shine",color:"red"}]

# What is directly in the beam's way, one block out in each direction the facing
# could mean. Whatever it is, if it is not air the beam stops at once.
execute if score @s ra.facing matches 0 unless block ~ ~-1 ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]
execute if score @s ra.facing matches 1 unless block ~ ~1 ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]
execute if score @s ra.facing matches 2 unless block ~ ~ ~-1 #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]
execute if score @s ra.facing matches 3 unless block ~ ~ ~1 #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]
execute if score @s ra.facing matches 4 unless block ~-1 ~ ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]
execute if score @s ra.facing matches 5 unless block ~1 ~ ~ #minecraft:air run tellraw @s [{text:"   the block it faces is not air, so the beam stops immediately",color:"yellow"}]

execute if data entity @s data.status.beam run tellraw @s [{text:"   last state: ",color:"gray"},{nbt:"data.status.beam",entity:"@s",color:"white"}]
