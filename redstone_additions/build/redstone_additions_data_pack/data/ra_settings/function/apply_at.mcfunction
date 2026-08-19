# /ra_settings:apply_at {p,r}
# Internal: write the consumed input into row r of page p. Context: as the player.

$execute unless data storage ra:settings pages[$(p)].rows[$(r)] run return 0
$data modify storage ra:settings cur set from storage ra:settings pages[$(p)].rows[$(r)]

# A number came back as a score and goes to the row's objective. Text cannot live
# on a scoreboard at all, so it goes to storage keyed by the player's name -- see
# ra_settings:user_str for what that costs.
execute if data storage ra:settings cur{type:"int"} run function ra_settings:apply_int with storage ra:settings cur
execute if data storage ra:settings cur{type:"str"} run function ra_settings:apply_str with storage ra:settings cur
