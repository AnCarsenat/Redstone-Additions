# /ra_wires:debug/generator_one
# One generator's report. Context: as its marker, at the block.
#
# Every line mirrors a condition the real code tests, in the order it tests them,
# so the first red line is the answer.
#
# It also runs the OLD test beside the new one. The generator used to find its
# fuel with `execute if items block ... container.*` and now walks Items itself;
# if those two ever disagree about the same barrel, that disagreement is a fact
# worth having, because the Breeder and the Liquid Drain still use the old one.

data modify storage ra:wires dbg set value {}
data modify storage ra:wires dbg.pos set from entity @s Pos
tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:wires",color:"white"}]

execute unless block ~ ~ ~ minecraft:barrel run tellraw @s [{text:"   NOT a barrel — placed by an older build. Break and replace it.",color:"red"}]

execute unless data storage ra:wires fuel_map run tellraw @s [{text:"   fuel_map MISSING — ra_wires:media/init did not run",color:"red"}]
execute if data storage ra:wires fuel_map run tellraw @s [{text:"   fuel_map loaded",color:"dark_gray"}]

execute unless data block ~ ~ ~ Items[0] run tellraw @s [{text:"   inventory empty",color:"red"}]
execute if data block ~ ~ ~ Items[0] run data modify storage ra:wires dbg.items set from block ~ ~ ~ Items
execute if data block ~ ~ ~ Items[0] run tellraw @s [{text:"   Items ",color:"gray"},{nbt:"dbg.items",storage:"ra:wires",color:"white"}]

# The new path: is slot 0's id a key in fuel_map?
data remove storage ra:wires dbg.hit
execute if data block ~ ~ ~ Items[0] run data modify storage ra:wires dbg.id set from block ~ ~ ~ Items[0].id
execute if data storage ra:wires dbg.id run function ra_wires:debug/generator_lookup with storage ra:wires dbg
execute if data storage ra:wires dbg.hit run tellraw @s [{text:"   slot 0 IS a fuel: ",color:"green"},{nbt:"dbg.hit.name",storage:"ra:wires",color:"yellow"},{text:"  ",color:"gray"},{nbt:"dbg.hit.ticks",storage:"ra:wires",color:"yellow"},{text:" ticks",color:"gray"}]
execute unless data storage ra:wires dbg.hit run tellraw @s [{text:"   slot 0 is not a fuel",color:"yellow"}]

# The old path, on the same barrel, for comparison.
execute if items block ~ ~ ~ container.* minecraft:coal run tellraw @s [{text:"   'if items container.*' also sees coal",color:"dark_gray"}]
execute unless items block ~ ~ ~ container.* minecraft:coal run tellraw @s [{text:"   'if items container.*' does NOT see coal — if there is coal above, that command is the bug, and the Breeder and Liquid Drain share it",color:"red"}]

execute if data entity @s data.data.burn run tellraw @s [{text:"   BURNING, ticks left ",color:"green"},{nbt:"data.data.burn",entity:"@s",color:"yellow"}]
execute unless data entity @s data.data.burn run tellraw @s [{text:"   not burning",color:"red"}]

function ra_lib:transport/net/read
tellraw @s [{text:"   grid ",color:"gray"},{score:{name:"@s",objective:"ra.tr.net"},color:"aqua"},{text:"  holds ",color:"gray"},{score:{name:"#net_amount",objective:"ra.tr.tmp"},color:"yellow"},{text:" of ",color:"gray"},{score:{name:"#net_capacity",objective:"ra.tr.tmp"},color:"yellow"},{text:" EU",color:"gray"}]
execute if score @s ra.tr.net matches ..0 run tellraw @s [{text:"   not on a grid at all",color:"red"}]
execute if score #net_amount ra.tr.tmp >= #net_capacity ra.tr.tmp run tellraw @s [{text:"   grid FULL — it burns but has nowhere to put EU. Add a Battery.",color:"yellow"}]

execute unless entity @e[type=block_display,tag=ra.skin.electric_generator,distance=..1] run tellraw @s [{text:"   no skin entity present",color:"yellow"}]
