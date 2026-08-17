# /ra_enchanting:recipes/match
# Ask every registered recipe list whether it wants this item.
# Input:  storage ra:enchant input   — the sacrificed item compound
# Output: storage ra:enchant result  — the item to produce (absent = no match)
#         storage ra:enchant chance  — success chance in percent

function #ra_enchanting:recipes

# A recipe that names no chance is a 5% recipe.
execute if data storage ra:enchant result unless data storage ra:enchant chance run data modify storage ra:enchant chance set value 5
