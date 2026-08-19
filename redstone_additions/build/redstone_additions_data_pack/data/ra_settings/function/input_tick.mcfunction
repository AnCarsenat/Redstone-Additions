# /ra_settings:input_tick
# Menu clicks, the admin dispatcher, and finished typed input.
# Called from ra:tick directly after ra_lib:input/tick, beside the Data Handler.
#
# WHY EVERY CLICK IS HANDLED HERE AND NOT IN ra_settings:tick
# A click can open an input session, and where in the tick that happens decides
# whether ra_lib:input sees the session on the tick it was born.
#
# ra_lib:input/tick runs partway through ra:tick. The Data Handler's dispatcher
# sits after it, so a session the Handler opens is first scanned on the NEXT tick.
# This dispatcher used to sit before it, so a session opened here was scanned in
# the same tick it was created -- against a brand new form, by a scan that was
# written on the assumption that a tick had passed. The Data Handler has never
# been able to reach that state, which is the whole reason its text input works
# and this did not.
#
# So the click handling moved to where the Handler's is. The seeding half stayed
# early, because it has to precede the module sounds it feeds. The two halves want
# opposite ends of the tick, and now each gets the end it needs.

scoreboard players enable @a ra.settings.open
scoreboard players enable @a ra.settings.act

execute as @a[scores={ra.settings.open=1..}] at @s run function ra_settings:open_click
execute as @a[scores={ra.settings.act=1..}] at @s run function ra_settings:act

scoreboard players enable @a[tag=ra.admin] ra.settings.admin
execute as @a[scores={ra.settings.admin=1..}] at @s run function ra_settings:admin_dispatch

execute as @a[scores={ra.settings.pend=1..}] at @s run function ra_settings:apply_pending

# After the dispatchers, so a preference changed by a click applies on the tick it
# was pressed rather than the one after.
function ra_settings:hooks
