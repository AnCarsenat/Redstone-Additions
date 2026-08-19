# /ra_settings:act/at {p,r}
# Internal: apply row r of page p. Context: as the player.
#
# Only user rows can arrive here. The menu is built from storage ra:settings
# pages, and tools/settings_gen.py puts nothing but user-scope rows in it -- so
# there is no operator setting on screen for a player to click, and no need for a
# permission check that would have to refuse them after the fact.
#
# Operator settings are reached with /function ra_settings:admin/..., which needs
# permission level 2. That is the gate, and the game enforces it rather than this
# pack having to.

$execute unless data storage ra:settings pages[$(p)].rows[$(r)] run return 0
$data modify storage ra:settings cur set from storage ra:settings pages[$(p)].rows[$(r)]
execute unless data storage ra:settings cur{scope:"user"} run return 0

# int and str are typed, not stepped, so they hand over to the input library and
# finish on a later tick. Everything else resolves now.
execute if data storage ra:settings cur{type:"str"} run return run function ra_settings:edit_start with storage ra:settings q
execute if data storage ra:settings cur{type:"int"} if score #dir ra.set.tmp matches 0 run return run function ra_settings:edit_start with storage ra:settings q

function ra_settings:act/user with storage ra:settings cur
function ra_settings:page/open
