# /ra_ender:link/send_fluid {channel:"...",medium:"...",rate:N}
# Context: as the sending vault marker.

# Take our slice out of the local network first.
$function ra_lib:transport/net/take {amount:$(rate),medium:"$(medium)"}
scoreboard players operation #ender.carry ra.temp = #net_moved ra.tr.tmp
execute if score #ender.carry ra.temp matches ..0 run return 0

# Offer it to the partner. A partner that takes less than everything leaves the
# remainder in #ender.carry.
$execute as @e[type=marker,tag=ra.ender.recv_fluid,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s run function ra_ender:link/receive_fluid with storage ra:ender fluid

# Whatever came back goes straight into the network it came from, so nothing is
# lost when the far side is full or holds another medium.
execute if score #ender.carry ra.temp matches 1.. run function ra_ender:link/return_fluid with storage ra:ender fluid
