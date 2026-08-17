# /ra_infinite:items/give_generator_casing
# Give the Generator Casing crafting item
#
# The base item is a repeating command block. Vanilla recipe ingredients match on
# item id alone — they cannot ask for custom_data — so the casing and each core
# are built on block items no survival player can obtain or place
# (GameMasterBlockItem: placing one needs creative *and* permission level 2).
# That is what keeps a bare netherite scrap from standing in for a casing, with
# no advancement guard in the way. The Item Crate uses a plain command block, so
# these four ids are deliberately different from it.

give @s minecraft:repeating_command_block[item_model="minecraft:copper_grate",item_name="Generator Casing",rarity=uncommon,enchantment_glint_override=true,lore=[{text:"Craft with a core to build a generator",color:"gray",italic:false}],custom_data={ra:{generator_casing:1b}}]
