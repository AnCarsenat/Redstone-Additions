# /ra_jetpacks:mode/power_toggle
# Switch the whole jetpack off, or back on, for one player.
# Context: as the player who ran /trigger ra.jp.power.
#
# Off means off: no hover, no sneak thrust, no fuel burn, no particles. The
# chestplate keeps its components, so nothing has to be re-fitted afterwards.

scoreboard players set @s ra.jp.power 0
scoreboard players enable @s ra.jp.power

# Read the old value before writing, or the test below flips it straight back.
scoreboard players set #jp.off ra.temp 0
execute if score @s ra.jp.off matches 1 run scoreboard players set #jp.off ra.temp 1

execute if score #jp.off ra.temp matches 0 run scoreboard players set @s ra.jp.off 1
execute if score #jp.off ra.temp matches 1 run scoreboard players set @s ra.jp.off 0

# Switching off has to give gravity back before the next tick runs.
execute if score @s ra.jp.off matches 1 run function ra_jetpacks:flight/stop

execute if score @s ra.jp.off matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"off",color:"red"},{text:" — /trigger ra.jp.power to switch it back on",color:"dark_gray"}]
execute unless score @s ra.jp.off matches 1 run title @s actionbar [{text:"Jetpack: ",color:"gray"},{text:"on",color:"green"}]
playsound minecraft:block.lever.click player @s ~ ~ ~ 0.6 1.4
