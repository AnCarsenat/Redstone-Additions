# /ra_settings:tick
# The half of the settings tick that must run EARLY. Called from ra:tick before
# the module ticks.
#
# Only seeding. The module ticks below this in ra:tick emit sounds and particles
# filtered on per-player scores, and a player with no score is excluded by those
# filters -- so the scores have to exist before anything tries to play a sound.
#
# Everything else -- menu clicks, the admin dispatcher, typed input -- lives in
# ra_settings:input_tick, which runs AFTER ra_lib:input/tick. See there for why
# that separation is not cosmetic.

function ra_settings:sync
