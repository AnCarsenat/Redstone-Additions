# /ra_jetpacks:kit/needs_jetpack
# Refuse an upgrade kit with nothing to upgrade. Context: as the player.

title @s actionbar [{text:"Fit a jetpack to your chestplate first",color:"red"}]
playsound minecraft:block.fire.extinguish player @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.6 0.6
