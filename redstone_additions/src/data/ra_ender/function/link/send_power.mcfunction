# /ra_ender:link/send_power {channel:"..."}
# Context: as the sending vault marker. Reads #ender.carry, writes #ender.sent.

scoreboard players set #ender.sent ra.temp 0
$execute as @e[type=marker,tag=ra.ender.recv_power,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s run function ra_ender:link/receive_power
