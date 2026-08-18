# /ra_lib:skin/spawn_with_light {skin,id,facing}
# Internal: merge the sampled light level in, then spawn.
#
# A macro cannot read a score, and the caller's own arguments are already a macro
# substitution, so the light has to join them in storage before the summon.

$data modify storage ra:temp skin.skin set value "$(skin)"
$data modify storage ra:temp skin.id set value "$(id)"
$data modify storage ra:temp skin.facing set value "$(facing)"
function ra_lib:skin/spawn with storage ra:temp skin
