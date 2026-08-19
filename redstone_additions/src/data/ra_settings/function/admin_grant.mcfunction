# /ra_settings:admin_grant
# Give this player lasting server-settings access. Context: as the player.
#
# WHY A TAG AT ALL
# The buttons on the settings pages fire /trigger rather than /function, because a
# run_command click event raises a confirmation prompt every time and an operator
# stepping a value ten times should not answer ten dialogs. /trigger does not
# prompt -- but it also does not check permissions, so the check has to live
# somewhere, and this tag is it.
#
# It can only be handed out by something that already needs permission level 2:
# this function, reached through ra_settings:admin/show, or /tag typed by an
# operator. No player can give it to themselves.
#
# It PERSISTS. That is the point -- a tagged player opens the panel directly,
# today and after the next reload. It also means it is a role rather than a
# session: removing someone\'s operator status does not remove this, and
# ra_settings:admin/revoke is how you take it back.

tag @s add ra.admin
scoreboard players enable @s ra.settings.admin
