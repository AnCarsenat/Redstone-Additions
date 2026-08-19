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

# Drawing a menu is what makes its buttons usable; see ra_settings:init.
scoreboard players set @s ra.settings.viewing 1200

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
# Shown to everyone, because nothing can tell operators apart: there is no
# permission-level selector, and @s[level=...] is EXPERIENCE level, which is a
# different number that happens to look plausible.
#
# It RUNS a trigger rather than suggesting a function. Suggesting /function is
# useless to somebody who cannot run one -- it puts text in their chat box that
# the game will refuse -- whereas the trigger works for everybody and lands in
# ra_settings:server_open, which opens the panel for a holder of ra.admin and
# explains itself to anyone else.
tellraw @s [{text:"  "},{text:"[ Server settings ]",color:"gray",hover_event:{action:"show_text",value:"World-wide settings — needs operator permission"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 2"}}]
