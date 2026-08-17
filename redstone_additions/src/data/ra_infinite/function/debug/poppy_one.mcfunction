# /ra_infinite:debug/poppy_one
# One generator's report. Context: as its marker, at the marker.

# Where it is and how it is turned.
data modify storage ra:debug poppy set value {}
data modify storage ra:debug poppy.pos set from entity @s Pos
data modify storage ra:debug poppy.rot set from entity @s Rotation
data modify storage ra:debug poppy.props set from entity @s data.properties

execute store result score #dbg.facing ra.temp run scoreboard players get @s ra.facing
execute store result score #dbg.cd ra.temp run scoreboard players get @s ra.cooldown

tellraw @p [{text:"— marker ",color:"gray"},{nbt:"poppy.pos",storage:"ra:debug",color:"white"},{text:" rot ",color:"gray"},{nbt:"poppy.rot",storage:"ra:debug",color:"white"}]
tellraw @p [{text:"  props ",color:"gray"},{nbt:"poppy.props",storage:"ra:debug",color:"white"},{text:"  facing ",color:"gray"},{score:{name:"#dbg.facing",objective:"ra.temp"},color:"white"},{text:"  cooldown ",color:"gray"},{score:{name:"#dbg.cd",objective:"ra.temp"},color:"white"}]

# What the block one step forward looks like.
execute positioned ^ ^ ^1 if block ~ ~ ~ #ra_infinite:growable run tellraw @p [{text:"  front: ",color:"gray"},{text:"growable",color:"green"}]
execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_infinite:growable run tellraw @p [{text:"  front: ",color:"gray"},{text:"blocked",color:"red"}]
execute positioned ^ ^ ^1 if block ~ ~-1 ~ #ra_infinite:flower_ground run tellraw @p [{text:"  below front: ",color:"gray"},{text:"flower_ground",color:"green"}]
execute positioned ^ ^ ^1 unless block ~ ~-1 ~ #ra_infinite:flower_ground run tellraw @p [{text:"  below front: ",color:"gray"},{text:"not soil",color:"red"}]
execute positioned ^ ^ ^1 if block ~ ~ ~ #minecraft:dirt run tellraw @p [{text:"  front is #minecraft:dirt",color:"dark_gray"}]
execute positioned ^ ^ ^1 if block ~ ~-1 ~ #minecraft:dirt run tellraw @p [{text:"  below front is #minecraft:dirt",color:"dark_gray"}]
execute positioned ^ ^ ^1 if block ~ ~-1 ~ minecraft:grass_block run tellraw @p [{text:"  below front is literally grass_block",color:"dark_gray"}]

# And the verdict the goggles use, over the whole 3x3.
scoreboard players set #poppy.ground ra.temp 0
execute positioned ^ ^ ^1 run function ra_infinite:blocks/poppy_generator/check_ground
tellraw @p [{text:"  ground verdict: ",color:"gray"},{score:{name:"#poppy.ground",objective:"ra.temp"},color:"yellow"},{text:"  (1 = will plant)",color:"dark_gray"}]
