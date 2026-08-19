# /ra_settings:open_click
# A page button was clicked, or the player typed /trigger ra.settings.open.
# Context: as the player.
#
# 1 is the player's own menu -- and also what a bare /trigger produces, since
# /trigger with no argument adds 1 to a score the dispatcher has just zeroed.
# 2 is the server settings, which go through server_open because whether they may
# be opened at all depends on the player. Page N arrives as N+3: 0 cannot be
# delivered through a trigger, and 1 and 2 are taken.

execute store result score #v ra.set.tmp run scoreboard players get @s ra.settings.open
scoreboard players set @s ra.settings.open 0
scoreboard players enable @s ra.settings.open

execute if score #v ra.set.tmp matches ..1 run return run function ra_settings:menu
execute if score #v ra.set.tmp matches 2 run return run function ra_settings:server_open

scoreboard players remove #v ra.set.tmp 3
scoreboard players operation @s ra.settings.page = #v ra.set.tmp
function ra_settings:page/open
