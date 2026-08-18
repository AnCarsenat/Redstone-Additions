# /ra_jetpacks:kit/read_fitted_tier_only
# The tier half of read_fitted, for callers that have already set #jp.n by hand.
# Context: as the player.

data modify storage ra:jetpacks fit.tier set value "iron"
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack_tier:"infinite"}}] run data modify storage ra:jetpacks fit.tier set value "infinite"
