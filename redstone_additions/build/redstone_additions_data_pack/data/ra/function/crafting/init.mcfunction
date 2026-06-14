# /ra_lib:crafting/init
# Initialize the crafting system
# Call this from your main load function

# Scoreboards for crafting
scoreboard objectives add ra.craft_id dummy

tellraw @a [{text:"[RA] ",color:"gold"},{text:"Crafting system initialized",color:"gray"}]
