# /ra_settings:placement/refuse
# Tell the placer their block is switched off.
# Context: as the placement bat, at the placement position.
#
# THE ITEM IS NOT RETURNED, THOUGH IT SHOULD BE
# This used to claim it was. It is not: a spawn egg is consumed by vanilla before
# the bat it spawns ever reaches this pack, and nothing here puts one back. A
# disabled block is an admin's policy rather than a mistake by the player holding
# it, so it costing them the item is wrong -- but saying so in a comment while the
# code did nothing was worse, because it read as handled.
#
# Doing it properly needs the item rebuilt from its block id, and the bat carries
# only the ra.place.<id> tag, not the components. That is a give_<id> lookup this
# pack does not have yet.

tellraw @a[tag=ra.placer,limit=1] [{text:"[Settings] ",color:"gold"},{text:"That block is disabled on this world.",color:"red"}]
playsound minecraft:block.note_block.bass block @a[tag=ra.placer,limit=1] ~ ~ ~ 0.6 0.7

# RUN, not suggest. This is aimed at whoever just tried to place the block, who is
# usually not an operator -- and a suggested /function is useless to somebody who
# cannot run one. A trigger works for everybody and needs no confirmation dialog.
tellraw @a[tag=ra.placer,limit=1] [{text:"  "},{text:"[ What else is disabled? ]",color:"yellow",hover_event:{action:"show_text",value:"List every block that cannot be placed here"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 3"}}]
