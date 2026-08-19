# /ra_settings:hooks
# Apply user settings that are not enforced at their own call sites.
# Called once per tick from ra_settings:tick, AFTER the click dispatcher, so a
# toggle takes effect on the tick it was pressed rather than the one after.
#
# Sounds and particles are gated in the commands themselves -- every playsound
# and particle in the pack carries the matching scores= filter -- so there is
# nothing to do for them here.
#
# DEBUG IS MIRRORED, NOT OWNED
# The pack gates 63 messages on the ra.debug tag, and that tag predates this
# system: /tag @s add ra.debug is still how ra_multiblock:blast_forge/debug_structure
# tells you to turn it on. An earlier version of this file set the tag from the
# score in both directions, which quietly stripped the tag from anyone who had
# added it by hand -- the setting fought the documented command and won, every
# tick.
#
# So the score only ever ADDS. Turning the setting off removes the tag once, on
# the tick it changes, and after that a hand-added tag is left alone. ra.u.dbg.was
# is what makes "changed" knowable without reading the score twice.

scoreboard objectives add ra.u.dbg.was dummy

tag @a[scores={ra.u.dbg=1..}] add ra.debug

# The falling edge only: on for a player whose setting was on last tick and is
# off now. A player who never used the setting has both at 0 and is untouched.
execute as @a[scores={ra.u.dbg=..0}] if score @s ra.u.dbg.was matches 1.. run tag @s remove ra.debug

execute as @a run scoreboard players operation @s ra.u.dbg.was = @s ra.u.dbg
