# /ra_ender:link/probe_power {channel:"..."}
# Read how much the partner's GRID is holding into #ender.theirs, or leave it
# at -1 when there is no partner on this channel.
# Context: as the asking vault marker.
#
# Grid totals, not vault buffers: in two-way mode the vault is levelling the two
# bases against each other, not two boxes that happen to sit between them.

$execute as @e[type=marker,tag=ra.ender.recv_power,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} run function ra_ender:link/probe_power_read
