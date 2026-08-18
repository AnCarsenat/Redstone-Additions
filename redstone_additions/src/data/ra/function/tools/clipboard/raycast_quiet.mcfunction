# /ra:tools/clipboard/raycast_quiet {action:"copy"|"paste"}
# Internal: the same four-step reach as raycast, without the "nothing there"
# complaint -- the caller treats a miss as meaningful rather than as an error.
# Context: as the player.

$execute at @s anchored eyes positioned ^ ^ ^1 as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/clipboard/$(action)
$execute at @s anchored eyes positioned ^ ^ ^2 unless data storage ra:temp clip_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/clipboard/$(action)
$execute at @s anchored eyes positioned ^ ^ ^3 unless data storage ra:temp clip_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/clipboard/$(action)
$execute at @s anchored eyes positioned ^ ^ ^4 unless data storage ra:temp clip_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/clipboard/$(action)
