# /ra_ender:link/send_items {channel:"...",recv:"tag"}
# Hand the pending stack to the nearest vault on this channel wearing `recv`.
# Context: as the sending vault marker, at its barrel.
#
# The channel is a string, so matching it needs a macro: no selector predicate can
# compare one entity's property with another's.
#
# The excluded tag is ra.ender.sending, held by whoever is pushing, not
# ra.ender.self, which marks the vault whose cycle is running. On a pull those are
# two different vaults — the one whose cycle it is *is* the receiver — and excluding
# self meant a pull could never deliver anything.

$execute as @e[type=marker,tag=$(recv),tag=!ra.ender.sending,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s run function ra_ender:link/receive_items
