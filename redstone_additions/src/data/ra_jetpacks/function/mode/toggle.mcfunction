# /ra_jetpacks:mode/toggle
# Flip between classic and hover.
# Context: as the player who ran /trigger ra.jp.mode.

scoreboard players set @s ra.jp.mode 0
scoreboard players enable @s ra.jp.mode

# The old value is read first: writing the state and then testing it again would
# flip it straight back.
scoreboard players set #jp.state ra.temp 0
execute if score @s ra.jp.state matches 1 run scoreboard players set #jp.state ra.temp 1

execute if score #jp.state ra.temp matches 0 run scoreboard players set @s ra.jp.state 1
execute if score #jp.state ra.temp matches 1 run scoreboard players set @s ra.jp.state 0

# Leaving hover has to give gravity back before the next tick runs.
function ra_jetpacks:flight/stop

execute if score @s ra.jp.state matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"hover",color:"aqua"},{text:" — sneak and look up or down",color:"dark_gray"}]
execute unless score @s ra.jp.state matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"classic",color:"green"},{text:" — sneak to rise",color:"dark_gray"}]
playsound minecraft:block.lever.click player @s ~ ~ ~ 0.6 1.4
