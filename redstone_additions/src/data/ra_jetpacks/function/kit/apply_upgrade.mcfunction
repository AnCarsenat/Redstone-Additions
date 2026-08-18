# /ra_jetpacks:kit/apply_upgrade {bit:4,key:"jp_scorch",kit:"scorch",name:"Scorch",hint:"burns what flies under you"}
# Fit one upgrade kit. Context: as the player, kit in the main hand.
#
# THE UPGRADE LIVES ON THE CHESTPLATE
# It used to live on the player, as a tag, on the reasoning that every other
# jetpack setting already does and that an item modifier cannot merge into an
# existing component. The first half was true and the second was the wrong
# conclusion: a tag on the player is not a property of the jetpack, so the
# upgrades followed you to a different chestplate, survived losing the jetpack
# entirely, and -- the way it actually showed up -- made every kit report itself
# already fitted for ever after the first one.
#
# The merge problem is real, and the answer is to not merge: read the chestplate's
# whole state, work out the new whole state, and write that. See write_fitted.

execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack:1b}}] run return run function ra_jetpacks:kit/needs_jetpack

$execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{$(key):1b}}] run return run function ra_jetpacks:kit/already_fitted

function ra_jetpacks:kit/read_fitted
$scoreboard players add #jp.n ra.temp $(bit)
function ra_jetpacks:kit/write_fitted

$clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,upgrade:"$(kit)"}}] 1

$title @s actionbar [{text:"$(name) kit fitted",color:"green"},{text:" - $(hint)",color:"gray"}]
particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.6 40
playsound minecraft:block.anvil.use player @a[distance=..16] ~ ~ ~ 0.8 1.4
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.8
