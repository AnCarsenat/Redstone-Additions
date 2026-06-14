# /data/ra_wireless/function/load.mcfunction
# RA Wireless — Initialize wireless redstone module scoreboards

# Scoreboard for channel storage
scoreboard objectives add ra.channel dummy

# Scoreboard for pulse timer (remote pulse duration)
scoreboard objectives add ra.pulse_timer dummy

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Wireless Redstone loaded!",color:"aqua"}]
