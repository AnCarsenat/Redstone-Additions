# /ra:tools/goggles/give
# Give Goggles tool to player
# Goggles reveal information about custom blocks and multiblocks

give @s leather_helmet[item_name="Goggles",custom_data={ra:{goggles:1b}},dyed_color=16750848,enchantment_glint_override=true,lore=[{text:"Reveals custom block info",color:"gray",italic:false},{text:"Wear or hold to scan nearby blocks",color:"dark_gray",italic:false}]]

tellraw @s [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Given ",color:"green"},{text:"Goggles",color:"gold"},{text:"!",color:"green"}]
