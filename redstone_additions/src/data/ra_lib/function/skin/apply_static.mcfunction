# /ra_lib:skin/apply_static {real:"minecraft:barrel",skin:"minecraft:...",id:"..."}
# Same as ra_lib:skin/apply, for a skin block that has no `facing` property.

# Centre-anchored kill, plus the corner-anchored one that clears pre-centre
# skins; see ra_lib:skin/apply.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute align xyz run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute unless block ~ ~ ~ $(real) run return 0
$function ra_lib:skin/spawn_static {skin:"$(skin)",id:"$(id)"}
return 1
