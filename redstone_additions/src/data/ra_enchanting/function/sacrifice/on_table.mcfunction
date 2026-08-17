# /ra_enchanting:sacrifice/on_table
# One sacrificed stack, resting on an enchanting table.
# Context: as the item entity, at the item entity.

# Four scan steps = one second between rolls, so a stack burns down visibly
# instead of vanishing in a single tick.
scoreboard players add @s ra.ench.cd 1
execute unless score @s ra.ench.cd matches 4.. run return 0
scoreboard players set @s ra.ench.cd 0

data modify storage ra:enchant input set from entity @s Item
data remove storage ra:enchant result
data remove storage ra:enchant chance
function ra_enchanting:recipes/match

# Nothing wants this item — leave the stack alone.
execute unless data storage ra:enchant result run return 0

function ra_enchanting:sacrifice/attempt
