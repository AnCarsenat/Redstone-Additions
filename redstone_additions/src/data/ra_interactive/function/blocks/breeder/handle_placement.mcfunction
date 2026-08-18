# /ra_interactive:blocks/breeder/handle_placement
# Handle breeder placement
# Context: as bat, at bat position

execute unless entity @s[tag=ra.place.breeder] run return 0

# A barrel, not a dispenser. A dispenser fires its own inventory on any rising
# redstone edge, so a breeder loaded with wheat spat the wheat across the field
# the moment you powered it. A barrel is the same inventory with none of that,
# and ra_lib:skin/apply puts the dispenser face back on the outside.
function ra_lib:placement/place {block_id:"minecraft:barrel",block_tag:"breeder",dir_type:2}

# Remove ra.new tag now that setup is complete
execute at @e[type=marker,tag=ra.custom_block.breeder,tag=ra.new,limit=1,sort=nearest] run function ra_interactive:blocks/breeder/refresh_display
tag @e[type=marker,tag=ra.custom_block.breeder,tag=ra.new] remove ra.new

return 1
