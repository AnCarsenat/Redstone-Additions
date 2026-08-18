# /ra:tools/wrench/open_menu
# Show the cycle menu for a block with more than one setting.
# Context: as the marker, at the block.
#
# The block is remembered on the PLAYER, as three scores, because a chat button
# is clicked some time after the menu is drawn and the click has no idea what it
# was aimed at. Scores are the only per-player storage a data pack has, and a
# position survives the player turning round -- which a re-raycast would not.
#
# ra.wrench.sel is on the marker only while the rows are being drawn: the nbt
# fields in a tellraw are resolved when the message is built, so the tag can come
# straight back off.

# Tagged BEFORE the position is remembered: remember_target looks the marker up
# by this tag, and running it first found nothing at all.
tag @s add ra.wrench.sel

execute as @a[tag=ra.wrench_user,limit=1] run function ra:tools/wrench/remember_target

tellraw @a[tag=ra.wrench_user,limit=1] [{text:""}]
tellraw @a[tag=ra.wrench_user,limit=1] [{text:"── ",color:"dark_gray"},{nbt:"data.type",entity:"@s",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]

scoreboard players set #wr.i ra.temp 0
function ra:tools/wrench/menu_row_next

tag @s remove ra.wrench.sel
