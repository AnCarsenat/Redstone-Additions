# /ra_jetpacks:load
# RA Jetpacks — chestplate upgrade kits

# Players switch flight mode with /trigger ra.jp.mode.
# The four jetpack triggers are NOT enabled here. flight/tick_player enables them
# for anyone actually wearing a jetpack, every tick, so enabling them for everyone
# at load only put four names in the /trigger completion of every player who has
# never seen one.
scoreboard objectives add ra.jp.mode trigger

# 0 = classic, 1 = hover.
scoreboard objectives add ra.jp.state dummy

# Ticks of powered flight since the last coal was burned.
scoreboard objectives add ra.jp.fuel dummy

# Hover servo: last sampled Y in thousandths of a block, and the thruster tier
# currently written onto the gravity attribute.
scoreboard objectives add ra.jp.y dummy

# Thruster kit: last sampled X/Z in thousandths, so the push can be measured
# from how far the player actually moved rather than from where they are looking.
# Scorch kit: ticks until the next direct hit, so burning is continuous while
# the damage is not twenty times a second.
scoreboard objectives add ra.jp.scorch_cd dummy

scoreboard objectives add ra.jp.x dummy
scoreboard objectives add ra.jp.z dummy

# Thruster kit: the SMOOTHED horizontal speed the push is built from, so a noisy
# single-tick delta cannot turn into a different-sized teleport every tick.
# The upgrade menu: /trigger ra.jp.kits opens it, and its buttons come back
# through the same trigger with a value. See ra_jetpacks:kit/menu_action.
scoreboard objectives add ra.jp.kits trigger

scoreboard objectives add ra.jp.vx dummy
scoreboard objectives add ra.jp.vz dummy
scoreboard objectives add ra.jp.grav dummy

# 1 once the player has actually left the ground, so touching down can be told
# apart from standing there.
scoreboard objectives add ra.jp.air dummy

# Master switch: /trigger ra.jp.power sets ra.jp.off, and a player with it set
# wears a jetpack that does nothing at all.
scoreboard objectives add ra.jp.off dummy
scoreboard objectives add ra.jp.power trigger

# Engine-loop timer, and the mute flag behind /trigger ra.jp.sound.
scoreboard objectives add ra.jp.snd dummy
scoreboard objectives add ra.jp.mute dummy
scoreboard objectives add ra.jp.sound trigger

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Jetpacks loaded!",color:"aqua"}]
