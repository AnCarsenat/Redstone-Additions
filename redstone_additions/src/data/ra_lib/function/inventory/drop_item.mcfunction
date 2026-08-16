# /ra_lib:inventory/drop_item
# Drop storage ra:inventory item on the floor at the current position.
#
# Summoned with a placeholder and then overwritten from storage, so the item's
# components cross verbatim instead of being rebuilt from a macro literal.

summon item ~ ~0.5 ~ {Item:{id:"minecraft:stone",count:1},PickupDelay:20,Tags:["ra.inv.overflow"]}
data modify entity @e[type=item,tag=ra.inv.overflow,distance=..2,limit=1,sort=nearest] Item set from storage ra:inventory item
tag @e[type=item,tag=ra.inv.overflow,distance=..2] remove ra.inv.overflow
