# /ra_settings:apply_str {key}
# Internal: store typed text for this player. Context: as the player.
#
# Text cannot live on a scoreboard, so a per-player string needs its own store,
# keyed by something that identifies the player. UUID rather than name: a name
# can change and would orphan the setting, and the UUID is the only identifier a
# data pack can actually read off a player.
#
# The UUID is an int array, which no `if data` predicate can be written against
# by hand -- but a macro substitutes it verbatim as [I;a,b,c,d], which is exactly
# the literal a path predicate needs. That is what makes this two lines instead
# of a lookup table.

data modify storage ra:settings put.key set from storage ra:settings cur.key
data modify storage ra:settings put.u set from entity @s UUID
function ra_settings:apply_str_at with storage ra:settings put
data remove storage ra:settings put
