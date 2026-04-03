# /ra_interactive:blocks/infinite_lava_cauldron/handle_placement
# Handle infinite lava cauldron placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.infinite_lava_cauldron] run return 0

# Place lava cauldron with marker entity
setblock ~ ~ ~ lava_cauldron
summon marker ~ ~ ~ {Tags:["ra.custom_block","ra.custom_block.infinite_lava_cauldron"]}
playsound minecraft:block.stone.place block @a[distance=..16] ~ ~ ~ 1 1

return 1
