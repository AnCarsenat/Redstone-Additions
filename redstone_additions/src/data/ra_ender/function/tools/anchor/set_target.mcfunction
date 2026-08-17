# /ra_ender:tools/anchor/set_target {level:N,id:"A"}
# Point one row of the nearest anchor's table at an id. Context: as a player.
#
# level is the redstone strength, 1-15. An empty id clears the row.

$scoreboard players set #ender.level ra.temp $(level)
execute unless score #ender.level ra.temp matches 1..15 run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"Level must be 1-15",color:"red"}]
execute unless entity @e[type=marker,tag=ra.custom_block.teleport_anchor,distance=..6] run return run tellraw @s [{text:"[RA] ",color:"gold"},{text:"No Teleport Anchor within 6 blocks",color:"red"}]

# The table index is one less than the signal.
scoreboard players remove #ender.level ra.temp 1
execute store result storage ra:ender set.i int 1 run scoreboard players get #ender.level ra.temp
$data modify storage ra:ender set.id set value "$(id)"

function ra_ender:tools/anchor/write_target with storage ra:ender set
$tellraw @s [{text:"[RA] ",color:"gold"},{text:"Signal ",color:"gray"},{text:"$(level)",color:"yellow"},{text:" now sends to id ",color:"gray"},{text:"$(id)",color:"aqua"}]
