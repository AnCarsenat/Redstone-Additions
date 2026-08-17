# /ra_jetpacks:mode/sound_toggle
# Mute or unmute the engine loop for one player.
# Context: as the player who ran /trigger ra.jp.sound.

scoreboard players set @s ra.jp.sound 0
scoreboard players enable @s ra.jp.sound

# Read the old value before writing, or the test below flips it straight back.
scoreboard players set #jp.mute ra.temp 0
execute if score @s ra.jp.mute matches 1 run scoreboard players set #jp.mute ra.temp 1

execute if score #jp.mute ra.temp matches 0 run scoreboard players set @s ra.jp.mute 1
execute if score #jp.mute ra.temp matches 1 run scoreboard players set @s ra.jp.mute 0

# Muting has to kill the sample that is already playing, not just stop new ones.
execute if score @s ra.jp.mute matches 1 at @s run function ra_jetpacks:flight/sound_off

execute if score @s ra.jp.mute matches 1 run title @s actionbar [{text:"Jetpack sound: ",color:"gray"},{text:"off",color:"red"}]
execute unless score @s ra.jp.mute matches 1 run title @s actionbar [{text:"Jetpack sound: ",color:"gray"},{text:"on",color:"green"}]
playsound minecraft:block.lever.click player @s ~ ~ ~ 0.6 1.4
