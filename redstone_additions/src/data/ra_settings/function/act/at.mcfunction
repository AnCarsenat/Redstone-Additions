# /ra_settings:act/at {p,r}
# Internal: apply row r of page p. Context: as the player.
#
# Only user rows reach here: tools/settings_gen.py puts nothing else in the menu
# registry, so there is no operator setting on screen for a player to click and be
# refused. Operator settings are /function ra_settings:admin/..., gated by the
# game at permission level 2.

$execute unless data storage ra:settings pages[$(p)].rows[$(r)] run return 0
$data modify storage ra:settings cur set from storage ra:settings pages[$(p)].rows[$(r)]
execute unless data storage ra:settings cur{scope:"user"} run return 0

# Typed rows hand over to ra_lib:input and finish on a later tick. The payload is
# copied field by field rather than substituted, because a row can carry a
# player-authored string and a macro would break on a quote character.
execute if data storage ra:settings cur{type:"str"} run function ra_settings:act/ask
execute if data storage ra:settings cur{type:"int"} if score #dir ra.set.tmp matches 0 run function ra_settings:act/ask
execute if data storage ra:settings cur{type:"str"} run return 0
execute if data storage ra:settings cur{type:"int"} if score #dir ra.set.tmp matches 0 run return 0

function ra_settings:act/user with storage ra:settings cur
function ra_settings:page/open
