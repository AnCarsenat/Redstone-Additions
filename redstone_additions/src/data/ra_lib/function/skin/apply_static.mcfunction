# /ra_lib:skin/apply_static {real:"minecraft:barrel",skin:"minecraft:...",id:"..."}
# Same as ra_lib:skin/apply, for a skin block that has no `facing` property.

$kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.9]
$execute unless block ~ ~ ~ $(real) run return 0
$function ra_lib:skin/spawn_static {skin:"$(skin)",id:"$(id)"}
return 1
