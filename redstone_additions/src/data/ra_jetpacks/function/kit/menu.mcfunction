# /ra_jetpacks:kit/menu
# Show the upgrade menu. Context: as the player.
#
# Reached with /trigger ra.jp.kits, and re-shown after every action so the menu
# behaves like a panel rather than a one-shot message.
#
# Everything it reports is read from the worn chestplate, because that is where
# fitted upgrades live. The one thing it keeps on the player is which of them are
# switched OFF -- see kit/toggle for why that side belongs to the player.

execute unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack:1b}}] run return run function ra_jetpacks:kit/needs_jetpack

tellraw @s [{text:""}]
tellraw @s [{text:"── ",color:"dark_gray"},{text:"Jetpack Upgrades",color:"gold",bold:true},{text:" ──",color:"dark_gray"}]

function ra_jetpacks:kit/menu_row {key:"jp_speed",mute:"ra.jp.mute_speed",label:"Thruster",toggle:11,drop:21}
function ra_jetpacks:kit/menu_row {key:"jp_lift",mute:"ra.jp.mute_lift",label:"Lift",toggle:12,drop:22}
function ra_jetpacks:kit/menu_row {key:"jp_scorch",mute:"ra.jp.mute_scorch",label:"Scorch",toggle:13,drop:23}

tellraw @s [{text:"Remove",color:"dark_gray"},{text:" gives the kit back as an item.",color:"dark_gray"}]
