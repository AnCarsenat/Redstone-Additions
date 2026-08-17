# /ra_infinite:items/give_poppy_core
# Give the Poppy Core crafting item
#
# In survival this comes out of an enchanting table: sacrifice poppies, 1% a
# piece. Base item is a chain command block — see
# ra_infinite:items/give_generator_casing for why.

give @s minecraft:chain_command_block[item_model="minecraft:flowering_azalea",item_name="Poppy Core",rarity=epic,enchantment_glint_override=true,lore=[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Poppy Generator",color:"gray",italic:false}],custom_data={ra:{poppy_core:1b}}]
