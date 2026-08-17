# /ra_ender:link/probe_power {channel:"..."}
# Read the partner's buffer into #ender.theirs, or leave it at -1.
# Context: as the asking vault marker.

$execute as @e[type=marker,tag=ra.ender.recv_power,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} store result score #ender.theirs ra.temp run data get entity @s data.data.eu
