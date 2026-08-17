# /ra_ender:link/probe_fluid {channel:"..."}
# Read the partner network's contents into #ender.theirs, or leave it at -1.
# Context: as the asking vault marker.

$execute as @e[type=marker,tag=ra.ender.recv_fluid,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} run function ra_ender:link/probe_fluid_read
