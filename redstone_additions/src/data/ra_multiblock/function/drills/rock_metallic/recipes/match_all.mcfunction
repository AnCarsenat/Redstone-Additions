# /ra_multiblock:blast_forge/recipes/match_all
# Try all recipe categories — sets storage ra:temp bf.result if matched
# Input: storage ra:temp bf.material (item compound from barrel)
# Output: storage ra:temp bf.result (item to produce) — absent if no match
#
# To add new recipes: create a new file in this folder and add a call here.

function ra_multiblock:blast_forge/recipes/smelting
execute if data storage ra:temp bf.result run return 1

function ra_multiblock:blast_forge/recipes/recycling
execute if data storage ra:temp bf.result run return 1

return 0
