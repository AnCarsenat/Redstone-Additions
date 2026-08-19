# /ra_enchanting:sacrifice/success
# The roll came up — hand out the upgraded item.
# Context: as the sacrificed item entity, at it.

# ra.ench.done keeps the product out of the next scan, so an upgrade that landed
# back on the same table is not immediately sacrificed again.
summon item ~ ~0.3 ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra","ra.ench.product","ra.ench.done"],PickupDelay:20s}
data modify entity @e[type=item,tag=ra.ench.product,distance=..0.6,limit=1,sort=nearest] Item set from storage ra:enchant result
tag @e[type=item,tag=ra.ench.product,distance=..0.6,limit=1,sort=nearest] remove ra.ench.product

particle minecraft:enchant ~ ~0.6 ~ 0.3 0.4 0.3 0.8 60 normal @a[scores={ra.u.par=1..}]
particle minecraft:end_rod ~ ~0.4 ~ 0.15 0.15 0.15 0.05 12 normal @a[scores={ra.u.par=1..}]
playsound minecraft:block.enchantment_table.use block @a[distance=..16,scores={ra.u.snd=1..}] ~ ~ ~ 1 1.2
playsound minecraft:entity.player.levelup block @a[distance=..16,scores={ra.u.snd=1..}] ~ ~ ~ 0.8 1.4
