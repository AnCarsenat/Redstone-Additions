# /ra_ender:tools/anchor/set_id {id:"A"}
# Name the nearest anchor within 6 blocks. Context: as a player.
#
# Ids are strings: "A", "base", "mine_2". Anything you can type.

execute unless entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6] run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]

$data modify entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6,limit=1,sort=nearest] data.properties.anchor_id set value "$(id)"
$tellraw @s [{text:"[RA] ",color:"gold"},{text:"Anchor id set to ",color:"gray"},{text:"$(id)",color:"aqua"}]
