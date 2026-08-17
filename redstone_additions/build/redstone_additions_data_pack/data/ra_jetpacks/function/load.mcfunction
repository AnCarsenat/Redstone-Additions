# /ra_jetpacks:load
# RA Jetpacks — chestplate upgrade kits

# Players switch flight mode with /trigger ra.jp.mode.
scoreboard objectives add ra.jp.mode trigger
scoreboard players enable @a ra.jp.mode

# 0 = classic, 1 = hover.
scoreboard objectives add ra.jp.state dummy

# Ticks of powered flight since the last coal was burned.
scoreboard objectives add ra.jp.fuel dummy

# Hover servo: last sampled Y in thousandths of a block, and the thruster tier
# currently written onto the gravity attribute.
scoreboard objectives add ra.jp.y dummy
scoreboard objectives add ra.jp.grav dummy

# 1 once the player has actually left the ground, so touching down can be told
# apart from standing there.
scoreboard objectives add ra.jp.air dummy

# Master switch: /trigger ra.jp.power sets ra.jp.off, and a player with it set
# wears a jetpack that does nothing at all.
scoreboard objectives add ra.jp.off dummy
scoreboard objectives add ra.jp.power trigger
scoreboard players enable @a ra.jp.power

# Engine-loop timer, and the mute flag behind /trigger ra.jp.sound.
scoreboard objectives add ra.jp.snd dummy
scoreboard objectives add ra.jp.mute dummy
scoreboard objectives add ra.jp.sound trigger
scoreboard players enable @a ra.jp.sound

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Jetpacks loaded!",color:"aqua"}]
