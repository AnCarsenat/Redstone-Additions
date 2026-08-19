# /ra_jetpacks:kit/already_fitted
# Refuse a duplicate upgrade. Context: as the player.
#
# Refusing rather than silently eating the kit: the upgrades do not stack, so a
# second one would be spent for nothing.

title @s actionbar [{text:"That upgrade is already fitted",color:"yellow"}]
playsound minecraft:block.fire.extinguish player @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.6 0.9
