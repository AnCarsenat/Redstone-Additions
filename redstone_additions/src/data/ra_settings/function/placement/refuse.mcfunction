# /ra_settings:placement/refuse
# Tell the placer their block is switched off, and give the item back.
# Context: as the placement bat, at the placement position.
#
# The item is returned rather than swallowed. A disabled block is an admin's
# policy, not a mistake by the player holding it, and taking the item as well
# would make the policy cost them something.

tellraw @a[tag=ra.placer,limit=1] [{text:"[Settings] ",color:"gold"},{text:"That block is disabled on this world.",color:"red"}]
playsound minecraft:block.note_block.bass block @a[tag=ra.placer,limit=1] ~ ~ ~ 0.6 0.7

# RUN, not suggest. This is aimed at whoever just tried to place the block, who is
# usually not an operator -- and a suggested /function is useless to somebody who
# cannot run one. A trigger works for everybody and needs no confirmation dialog.
tellraw @a[tag=ra.placer,limit=1] [{text:"  "},{text:"[ What else is disabled? ]",color:"yellow",hover_event:{action:"show_text",value:"List every block that cannot be placed here"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 3"}}]
