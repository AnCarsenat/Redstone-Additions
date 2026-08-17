# /ra_ender:tools/anchor/show
# Print the nearest anchor's id and target table. Context: as a player.

execute unless entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6] run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]

data modify storage ra:ender show set value {}
data modify storage ra:ender show.id set from entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.anchor_id
data modify storage ra:ender show.targets set from entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.targets

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor id ",color:"gray"},{nbt:"show.id",storage:"ra:ender",color:"aqua"}]
tellraw @s [{text:"  targets 1-15: ",color:"gray"},{nbt:"show.targets",storage:"ra:ender",color:"white"}]
