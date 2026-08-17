# /ra_ender:link/request_items {channel:"...",recv:"tag"}
# Find a vault on this channel that is willing to give, and run its push.
# Context: as the vault that wants a refill.

$execute as @e[type=marker,tag=ra.ender.send_item,tag=!ra.ender.self,limit=1,sort=nearest] if data entity @s data.properties{channel:"$(channel)"} at @s run function ra_ender:blocks/item_vault/push_to_puller
