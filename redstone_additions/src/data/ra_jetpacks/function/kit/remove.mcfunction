# /ra_jetpacks:kit/remove {bit,key,kit,label}
# Take one upgrade off the chestplate and hand the kit back. Context: as the player.
#
# The same read-whole-state / write-whole-state route fitting uses, with the bit
# cleared instead of set -- see kit/write_fitted for why an item modifier cannot
# simply unset one flag.

$execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{$(key):1b}}] run return 0

function ra_jetpacks:kit/read_fitted
$scoreboard players remove #jp.n ra.temp $(bit)
function ra_jetpacks:kit/write_fitted

# Switched-off state goes with it, or refitting the kit later would arrive muted
# for no visible reason.
$execute if entity @s[tag=ra.jp.mute_$(kit)] run tag @s remove ra.jp.mute_$(kit)

$function ra_jetpacks:items/give_$(kit)_kit

$title @s actionbar [{text:"$(label) kit removed",color:"yellow"}]
particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.4 25
playsound minecraft:block.grindstone.use player @s ~ ~ ~ 0.7 1.2
