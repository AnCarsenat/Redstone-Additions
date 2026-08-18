# /ra_lib:skin/spawn_static_with_light {skin,id}
# Internal: merge the sampled light level in, then spawn the unoriented variant.

$data modify storage ra:temp skin.skin set value "$(skin)"
$data modify storage ra:temp skin.id set value "$(id)"
function ra_lib:skin/spawn_static with storage ra:temp skin
