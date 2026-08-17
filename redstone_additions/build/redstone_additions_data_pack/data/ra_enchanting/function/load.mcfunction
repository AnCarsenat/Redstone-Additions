# /ra_enchanting:load
# RA Enchanting — sacrifice crafting on a vanilla enchanting table

# Ticks (in scan steps, not game ticks) since a sacrificed stack last rolled.
scoreboard objectives add ra.ench.cd dummy

# Counts game ticks between item scans, so the scan is not paid every tick.
scoreboard objectives add ra.ench.scan dummy
scoreboard players set #scan ra.ench.scan 0

data merge storage ra:enchant {input:{},chance:0}
data remove storage ra:enchant result

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Enchant crafting loaded!",color:"light_purple"}]
