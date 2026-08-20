# /ra_settings:user_str {key:"...",default:"..."}
# This player's value for a text setting, into storage ra:settings out.
# Context: as the player.
#
# The default stands until the player has actually typed something, the same way
# ra_settings:user treats an absent score -- an unset setting and a setting the
# player deliberately emptied are not the same thing.

# A macro whose arguments are incomplete fails without running a single one of
# its lines, so a row that omits `default` used to take its whole row down with
# it -- the setting simply did not appear. Fill it in first.
execute unless data storage ra:settings cur.default run data modify storage ra:settings cur.default set value ""

data modify storage ra:settings put.key set from storage ra:settings cur.key
data modify storage ra:settings put.u set from entity @s UUID
$data modify storage ra:settings out set value "$(default)"
function ra_settings:user_str_at with storage ra:settings put
data remove storage ra:settings put
