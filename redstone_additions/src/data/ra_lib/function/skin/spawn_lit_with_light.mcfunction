# /ra_lib:skin/spawn_lit_with_light {skin,id,facing,lit}
# Internal: merge the sampled light level in, then spawn the lit variant.

$data modify storage ra:temp skin.skin set value "$(skin)"
$data modify storage ra:temp skin.id set value "$(id)"
$data modify storage ra:temp skin.facing set value "$(facing)"
$data modify storage ra:temp skin.lit set value "$(lit)"
function ra_lib:skin/spawn_lit with storage ra:temp skin
