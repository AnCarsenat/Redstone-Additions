
# /ra_multiblock:drills/rock_metallic/check_blocks
# MACRO FUNCTION â€” Unified block pattern check for drill (rock metallic)
# Called: function ... with storage ra:multiblock drill_dir.{north|south|east|west}
# Context: at multiblock base/marker position
# Returns: 1 if all blocks valid, 0 if any block invalid
# NO side effects â€” does not modify storage or scoreboards

# === Functional blocks (relative to base) ===
$execute unless block $(base) minecraft:waxed_copper_block run return 0
$execute unless block $(input) minecraft:barrel run return 0
$execute unless block $(chain) minecraft:iron_bars run return 0
$execute unless block $(stone) minecraft:smooth_stone run return 0

# All checks passed
return 1
