# /ra_settings:apply_edit
# Route a consumed value to whatever it was for. Context: as the player.
#
# One entry point per kind, chosen from the payload rather than from where the
# request came in. That is what lets an operator's page and a player's menu share
# this whole path instead of each carrying a copy of it.

execute if data storage ra:settings edit{kind:"global"} run function ra_settings:apply/global with storage ra:settings edit
execute if data storage ra:settings edit{kind:"prop"} run function ra_settings:apply/prop with storage ra:settings edit
execute if data storage ra:settings edit{kind:"user"} run function ra_settings:apply/user with storage ra:settings edit

data remove storage ra:settings edit

execute if score @s ra.settings.apage matches 0.. run function ra_settings:admin_refresh
execute unless score @s ra.settings.apage matches 0.. run function ra_settings:page/open
