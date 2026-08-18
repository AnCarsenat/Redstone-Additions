# /ra_jetpacks:kit/write_fitted
# Write #jp.n ra.temp back onto the worn chestplate. Context: as the player.
#
# WHY SIXTEEN GENERATED ITEM MODIFIERS
# An item modifier is a static JSON file: it cannot read what is already on the
# item, and `set_components` replaces a component whole rather than merging into
# it. Writing "add the Scorch flag" is therefore not expressible -- the only
# thing that can be expressed is "the item now has exactly this custom_data and
# exactly this lore".
#
# So every reachable state gets its own file: two tiers times eight upgrade
# combinations. They are generated rather than hand-written, the state is a
# three-bit number, and the file is chosen by pasting that number into the
# modifier's name. Adding a fourth kit doubles the count, which is the honest
# cost of the item being the record instead of the player.

function ra_jetpacks:kit/read_fitted_tier_only
execute store result storage ra:jetpacks fit.n int 1 run scoreboard players get #jp.n ra.temp
function ra_jetpacks:kit/apply_fit with storage ra:jetpacks fit
