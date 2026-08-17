# /ra_infinite:items/give_mineral_core
# Give the Mineral Core crafting item
#
# In survival this comes out of an enchanting table: sacrifice stone, 1% a piece.
# Base item is a jigsaw — see ra_infinite:items/give_generator_casing for why
# each core sits on its own unplaceable block item.

give @s minecraft:jigsaw[item_model="minecraft:deepslate_diamond_ore",item_name="Mineral Core",rarity=epic,enchantment_glint_override=true,lore=[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Mineral Generator",color:"gray",italic:false}],custom_data={ra:{mineral_core:1b}}]
