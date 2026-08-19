# /ra_settings:open_click
# A page button was clicked, or the player typed /trigger ra.settings.open.
# Context: as the player.
#
# 1 is the root menu -- and also what a bare /trigger produces, since /trigger
# with no argument adds 1 to a score that the dispatcher has just zeroed. Page N
# arrives as N+2, because 0 cannot be delivered through a trigger at all and 1 is
# taken.

execute store result score #v ra.set.tmp run scoreboard players get @s ra.settings.open
scoreboard players set @s ra.settings.open 0
scoreboard players enable @s ra.settings.open

execute if score #v ra.set.tmp matches ..1 run return run function ra_settings:menu

scoreboard players remove #v ra.set.tmp 2
scoreboard players operation @s ra.settings.page = #v ra.set.tmp
function ra_settings:page/open
