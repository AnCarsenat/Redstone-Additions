# /ra_jetpacks:flight/out_of_fuel
# No coal left. The jetpack cuts out until some turns up.
# Context: as the player.

function ra_jetpacks:flight/stop
tag @s add ra.jp.dry

title @s actionbar [{text:"Jetpack out of fuel",color:"red"},{text:" — carry coal",color:"gray"}]
playsound minecraft:block.fire.extinguish player @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.8 0.6
