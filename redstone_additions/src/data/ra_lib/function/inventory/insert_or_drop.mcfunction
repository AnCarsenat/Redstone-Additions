# /ra_lib:inventory/insert_or_drop {id:"minecraft:...",count:N,components:{...}}
# Insert into the container at the current position, dropping whatever does not
# fit as an item entity. Returns the number actually inserted.
#
# `loot insert` silently DESTROYS anything the container cannot hold. Every
# caller that hands it a whole stack is therefore one full output container away
# from deleting a player's items with no message and no trace. This wrapper
# recovers the difference between what was offered and what went in, so a full
# destination costs the player a pickup rather than the items.

$scoreboard players set #inv_want ra.temp $(count)
$execute store result score #inv_put ra.temp run function ra_lib:inventory/insert {id:"$(id)",count:$(count),components:$(components)}

scoreboard players operation #inv_left ra.temp = #inv_want ra.temp
scoreboard players operation #inv_left ra.temp -= #inv_put ra.temp
execute if score #inv_left ra.temp matches ..0 run return run scoreboard players get #inv_put ra.temp

# Spawn one, then set the real count, so the components travel verbatim instead
# of being rebuilt from a macro-substituted literal.
$summon item ~ ~0.5 ~ {Item:{id:"$(id)",count:1,components:$(components)},PickupDelay:20,Tags:["ra.inv.overflow"]}
execute store result entity @e[type=item,tag=ra.inv.overflow,distance=..2,limit=1,sort=nearest] Item.count int 1 run scoreboard players get #inv_left ra.temp
tag @e[type=item,tag=ra.inv.overflow,distance=..2] remove ra.inv.overflow

return run scoreboard players get #inv_put ra.temp
