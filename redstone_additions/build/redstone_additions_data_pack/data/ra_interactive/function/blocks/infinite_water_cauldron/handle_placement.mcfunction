# /ra_interactive:blocks/infinite_water_cauldron/handle_placement
# Handle infinite water cauldron placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.infinite_water_cauldron] run return 0

# Place water cauldron (full) with marker entity (dir_type:0 = no direction)
setblock ~ ~ ~ water_cauldron[level=3]
summon marker ~ ~ ~ {Tags:["ra.custom_block","ra.custom_block.infinite_water_cauldron"]}
playsound minecraft:block.stone.place block @a[distance=..16] ~ ~ ~ 1 1

return 1
