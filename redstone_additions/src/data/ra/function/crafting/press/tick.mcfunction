# /ra_lib:crafting/press/tick
# Tick function for press crafting
# Call this from your main tick function
# 
# Press crafting works by dropping items on a smithing_table and pressing with a piston.
# When the piston head is detected above smithing_table, check for matching recipes.
#
# To register a recipe, call ra_lib:crafting/press/check_recipe with these macros:
#   - item1_data: NBT matcher for first item (e.g., {custom_item:1b})
#   - item2_data: NBT matcher for second item (e.g., {other_item:1b})
#   - result_id: The item ID for the result
#   - result_components: Components for the result item
#   - craft_tag: Unique tag for this recipe (e.g., "my_mod.my_item_craft")

# Run registered press recipe checks
function #ra_lib:crafting/press/recipes
