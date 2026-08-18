# /ra_jetpacks:kit/on_use
# A jetpack kit was right-clicked. As player, from the advancement.

advancement revoke @s only ra_jetpacks:kit_use

# The kit is a consumable, so the trigger fires on every tick of the right-click
# and would fit — and eat — one kit per tick. Same held/clicked pair the core
# tools use; ra_jetpacks:tick clears the tags once the button is released.
tag @s add ra.jp.kit_clicked
execute if entity @s[tag=ra.jp.kit_active] run return fail
tag @s add ra.jp.kit_active

execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"iron"}}] run return run function ra_jetpacks:kit/apply_iron
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"infinite"}}] run return run function ra_jetpacks:kit/apply_infinite

# Upgrade kits. They fit onto a jetpack that is already worn rather than onto a
# bare chestplate, so they are checked after the two that create one.
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{jetpack_kit:1b,upgrade:"speed"}}] run return run function ra_jetpacks:kit/apply_upgrade {bit:1,key:"jp_speed",kit:"speed",name:"Thruster",hint:"faster in a straight line"}
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{jetpack_kit:1b,upgrade:"lift"}}] run return run function ra_jetpacks:kit/apply_upgrade {bit:2,key:"jp_lift",kit:"lift",name:"Lift",hint:"climbs and sinks faster"}
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{ra:{jetpack_kit:1b,upgrade:"scorch"}}] run return run function ra_jetpacks:kit/apply_upgrade {bit:4,key:"jp_scorch",kit:"scorch",name:"Scorch",hint:"burns what flies under you"}
