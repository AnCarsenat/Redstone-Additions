# /ra_settings:menu
# The player's own preferences. Context: as a player.
#
# Reached with /trigger ra.settings.open set 1, so it works for someone with no
# permissions at all -- which is the entire reason this menu exists separately
# from the operator settings.
#
# Only user-scope rows are here. World settings are not hidden from players out
# of secrecy; they are absent because a button a player is only going to be
# refused is worse than no button.

tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{text:"Your Redstone Additions Preferences",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]

# With a single page there is nothing to choose between, so showing a menu whose
# only content is one button is a click that teaches the player nothing. Open it.
execute unless data storage ra:settings pages[1] run scoreboard players set @s ra.settings.page 0
execute unless data storage ra:settings pages[1] run return run function ra_settings:page/open

scoreboard players set #idx ra.set.tmp 0
data modify storage ra:settings scan set from storage ra:settings pages
function ra_settings:page/list
data remove storage ra:settings scan

tellraw @s [{text:"These affect only you.",color:"dark_gray"}]
# Shown to everyone rather than only to operators, because nothing can tell them
# apart: there is no permission-level selector, and @s[level=...] is EXPERIENCE
# level, which is a different number that happens to look plausible. A player
# without permission who runs it is told no by the game, which is the right
# answer from the right place.
tellraw @s [{text:"Operators: ",color:"dark_gray"},{text:"/function ra_settings:admin/show",color:"yellow",hover_event:{action:"show_text",value:"World-wide settings (needs permission level 2)"},click_event:{action:"suggest_command",command:"/function ra_settings:admin/show"}}]
