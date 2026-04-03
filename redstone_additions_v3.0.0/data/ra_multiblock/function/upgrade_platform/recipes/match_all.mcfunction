# /ra_multiblock:upgrade_platform/recipes/match_all
# Try all upgrade recipes — sets storage ra:temp upgrade.result if matched
# Input: storage ra:temp upgrade.input (item compound from dropped item entity)
# Output: storage ra:temp upgrade.result (item to produce) — absent if no match

data remove storage ra:temp upgrade.chance
function ra_multiblock:upgrade_platform/recipes/tier_upgrades
execute if data storage ra:temp upgrade.result run return 1

return 0
