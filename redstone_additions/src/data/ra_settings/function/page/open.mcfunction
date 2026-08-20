# /ra_settings:page/open
# Draw the page this player selected. Context: as the player.
# Reads which page from ra.settings.page, so a row click can find its way back here.

execute store result storage ra:settings q.p int 1 run scoreboard players get @s ra.settings.page
function ra_settings:page/open_at with storage ra:settings q
