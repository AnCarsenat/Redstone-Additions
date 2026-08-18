# /ra_lib:skin/apply_static {real:"minecraft:barrel",skin:"minecraft:...",id:"..."}
# Same as ra_lib:skin/apply, for a skin block that has no `facing` property.

# Centre-anchored kill, plus the corner-anchored one that clears pre-centre
# skins; see ra_lib:skin/apply.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute align xyz run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute unless block ~ ~ ~ $(real) run return 0

# Sampled one block above -- see ra_lib:skin/apply for why the display's own
# space is always dark and why a hardcoded brightness was wrong beside a torch.
execute positioned ~ ~1 ~ run function ra_lib:skin/light_here
execute store result storage ra:temp skin.block_light int 1 run scoreboard players get #skin_light ra.temp
$function ra_lib:skin/spawn_static_with_light {skin:"$(skin)",id:"$(id)"}
return 1
