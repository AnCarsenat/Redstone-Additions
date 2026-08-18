# /ra_ender:blocks/item_vault/give
# Give one Ender Item Vault.

give @s bat_spawn_egg[item_model="minecraft:barrel",item_name="Ender Item Vault",rarity=rare,enchantment_glint_override=true,lore=[{text:"Shares its contents with the vault on its channel",color:"gray",italic:false},{text:"Shift+RMB with the wrench: link / send / receive",color:"dark_gray",italic:false}],custom_data={ra:{ender_item_vault:1b}},entity_data={id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.ender_item_vault"],Silent:1b,NoAI:1b,Invulnerable:1b}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Ender Item Vault",color:"light_purple"},{text:" — set the channel with the Data Handler",color:"gray"}]
