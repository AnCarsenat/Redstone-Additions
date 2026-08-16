# /data/ra_wireless/function/load.mcfunction
# RA Wireless — Initialize wireless redstone module scoreboards

# Scoreboard for pulse timer (remote pulse duration)
scoreboard objectives add ra.pulse_timer dummy

# Marks a player waiting on a book to name a remote's channel.
scoreboard objectives add ra.remote.pending dummy

# Hotbar slot the remote occupied when its channel prompt opened.
scoreboard objectives add ra.remote.slot dummy

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Wireless Redstone loaded!",color:"aqua"}]
