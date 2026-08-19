# /ra_settings:tick
# Collect settings menu clicks and finished input. Called once per tick from ra:tick.
#
# Triggers are the only control surface a non-operator has: /trigger writes to a
# score they could not otherwise touch, and only while the objective is enabled
# for them. Re-enabling every tick is what makes a button clickable more than
# once -- a trigger disables itself the moment it is used.

scoreboard players enable @a ra.settings.open
scoreboard players enable @a ra.settings.act

execute as @a[scores={ra.settings.open=1..}] at @s run function ra_settings:open_click
execute as @a[scores={ra.settings.act=1..}] at @s run function ra_settings:act
# Both signs: a positive pending is a player editing a row of their own menu, -1
# is an operator editing a global setting from the admin tree. They share the
# consume path because the waiting and the timeout logic is identical.
execute as @a[scores={ra.settings.pend=1..}] at @s run function ra_settings:apply_pending
execute as @a[scores={ra.settings.pend=..-1}] at @s run function ra_settings:apply_pending
