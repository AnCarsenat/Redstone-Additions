# /ra_ender:link/request_shared {channel:"..."}
# Ask the unoccupied holder on this channel for one stack, for merging into a
# shared vault that already has something in it.
# Context: as the claiming vault marker, at its barrel.

$execute as @e[type=marker,tag=ra.ender.share,tag=!ra.ender.self,tag=!ra.ender.occupied,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s run function ra_ender:blocks/item_vault/push_to_puller
