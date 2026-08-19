# /ra_settings:admin_grant
# Mark this player as holding a server-settings session. Context: as the player.
#
# WHY A TAG AT ALL
# The buttons on the settings pages fire /trigger rather than /function, because
# a run_command click event raises a confirmation prompt every time and an
# operator stepping a value ten times should not answer ten dialogs. /trigger
# does not prompt -- but it also does not check permissions, so the check has to
# be somewhere, and this tag is it.
#
# The tag can only be obtained by reaching ra_settings:admin/show, and reaching
# any function needs permission level 2. A player who is not an operator cannot
# get it, and the objective is only enabled for players who hold it, so a
# hand-typed /trigger from anyone else is refused by the game before it arrives.
#
# TWO THINGS BOUND IT, because a tag outlives the permission that granted it:
#   - ra:load clears it from everybody, so a reload ends every session.
#   - It expires. Somebody de-opped while holding it stops being able to use it
#     within ra.admin.ttl ticks rather than keeping it until they log out.
# Neither is free of a gap; both are far shorter than "forever".

tag @s add ra.admin
scoreboard players set @s ra.admin.ttl 6000
scoreboard players enable @s ra.settings.admin
