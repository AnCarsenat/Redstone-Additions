# /ra_infinite:items/give_nether_core
# Give the Nether Core crafting item
#
# In survival this comes out of an enchanting table: sacrifice netherrack, 1% a
# piece. Base item is a structure block — see
# ra_infinite:items/give_generator_casing for why.

give @s minecraft:structure_block[item_model="minecraft:ancient_debris",item_name="Nether Core",rarity=epic,enchantment_glint_override=true,lore=[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Nether Generator",color:"gray",italic:false}],custom_data={ra:{nether_core:1b}}]
