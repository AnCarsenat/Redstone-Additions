# /ra_settings:hooks
# Apply user settings that are not enforced at their own call sites.
# Called once per tick from ra_settings:tick.
#
# Sounds and particles are gated in the commands themselves -- every playsound
# and particle in the pack carries the matching scores= filter, so there is
# nothing to do for them here.
#
# Debug messages are different. The pack already gates 63 of them on the ra.debug
# tag, which existed long before this system, so the setting is wired by keeping
# that tag in step with the score rather than by touching 63 call sites. One
# switch, and every message that already respected it keeps doing so.

tag @a[scores={ra.u.dbg=1..},tag=!ra.debug] add ra.debug
tag @a[scores={ra.u.dbg=..0},tag=ra.debug] remove ra.debug
