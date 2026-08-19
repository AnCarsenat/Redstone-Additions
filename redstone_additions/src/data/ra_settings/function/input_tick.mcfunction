# /ra_settings:input_tick
# Collect finished typed input. Called from ra:tick directly after
# ra_lib:input/tick, alongside ra:tools/data_handler/tick.
#
# The position matters enough to be its own function. The menu half of the
# settings tick has to run EARLY, before the module ticks whose sounds and
# particles are filtered on per-player scores it seeds. The input half has to run
# LATE, after ra_lib:input/tick has had the chance to notice a finished form --
# which is exactly where the Data Handler collects its own, and the Data Handler
# is the part of this that was never broken.

execute as @a[scores={ra.settings.pend=1..}] at @s run function ra_settings:apply_pending
