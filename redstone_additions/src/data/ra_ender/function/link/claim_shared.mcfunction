# /ra_ender:link/claim_shared {channel:"..."}
# Find the vault on this channel that currently holds the contents and take them.
# Context: as the claiming vault marker, at its barrel. Its coordinates are in
# storage ra:ender move.
#
# A holder with someone standing at it keeps what it has: two people at two ends
# of one channel each keep whatever is in front of them rather than having it
# yanked away mid-click.

$execute as @e[type=marker,tag=ra.ender.share,tag=!ra.ender.self,tag=!ra.ender.occupied,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s if data block ~ ~ ~ Items[0] run function ra_ender:link/hand_over with storage ra:ender move
