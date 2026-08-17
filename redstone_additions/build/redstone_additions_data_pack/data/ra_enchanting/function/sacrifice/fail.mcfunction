# /ra_enchanting:sacrifice/fail
# The roll missed — the item is gone, nothing comes back.
# Context: as the sacrificed item entity, at it.

particle minecraft:lava ~ ~0.4 ~ 0.2 0.2 0.2 0.05 10
particle minecraft:smoke ~ ~0.4 ~ 0.2 0.2 0.2 0.02 12
playsound minecraft:block.fire.extinguish block @a[distance=..16] ~ ~ ~ 1 0.6
playsound minecraft:entity.item.break block @a[distance=..16] ~ ~ ~ 0.8 0.8
