# /ra_settings:admin_refresh
# Redraw the settings page this player is looking at. Context: as the player.
#
# Changing a value used to print one line and leave the page above it showing the
# old numbers, so the only way to see what you had done was to reopen the page.
# Every action redraws instead.
#
# Nothing happens if the player is not on a page -- an action run by typing its
# /function path directly has no page to return to, and inventing one would be
# worse than the single confirmation line it already prints.

execute unless score @s ra.settings.apage matches 0.. run return 0

execute store result storage ra:settings q.a int 1 run scoreboard players get @s ra.settings.apage
function ra_settings:admin_refresh_at with storage ra:settings q
