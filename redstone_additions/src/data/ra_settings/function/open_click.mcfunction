# /ra_settings:open_click
# A page button was clicked, or the player typed /trigger ra.settings.open.
# Context: as the player.
#
# 1 is the player's own menu -- and also what a bare /trigger produces, since
# /trigger with no argument adds 1 to a score the dispatcher has just zeroed.
# 2 is the server settings, which go through server_open because whether they may
# be opened at all depends on the player. 3 is the disabled-blocks list, which
# anyone may look at. Page N arrives as N+4: 0 cannot be delivered through a
# trigger, and 1, 2 and 3 are taken.
#
# These ride the existing objective rather than getting their own. A trigger is
# per-player state the server enables and the player spends, and every extra one
# is another thing to enable every tick for everybody -- so a fixed code on a
# trigger that already exists beats a new objective for every new entry point.

execute store result score #v ra.set.tmp run scoreboard players get @s ra.settings.open
scoreboard players set @s ra.settings.open 0
scoreboard players enable @s ra.settings.open

execute if score #v ra.set.tmp matches ..1 run return run function ra_settings:menu
execute if score #v ra.set.tmp matches 2 run return run function ra_settings:server_open
execute if score #v ra.set.tmp matches 3 run return run function ra_settings:disabled

scoreboard players remove #v ra.set.tmp 4
scoreboard players operation @s ra.settings.page = #v ra.set.tmp
function ra_settings:page/open
