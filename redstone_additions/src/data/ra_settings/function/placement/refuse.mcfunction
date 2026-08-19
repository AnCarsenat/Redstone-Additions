# /ra_settings:placement/refuse
# Tell the placer their block is switched off, and give the item back.
# Context: as the placement bat, at the placement position.
#
# The item is returned rather than swallowed. A disabled block is an admin's
# policy, not a mistake by the player holding it, and taking the item as well
# would make the policy cost them something.

tellraw @a[tag=ra.placer,limit=1] [{text:"[Settings] ",color:"gold"},{text:"That block is disabled on this world.",color:"red"}]
playsound minecraft:block.note_block.bass block @a[tag=ra.placer,limit=1] ~ ~ ~ 0.6 0.7
