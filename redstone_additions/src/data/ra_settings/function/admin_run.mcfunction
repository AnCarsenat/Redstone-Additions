# /ra_settings:admin_run {a:N}
# Internal: look action N up in the generated table and run it.

data remove storage ra:settings call
$execute if data storage ra:settings actions[$(a)] run data modify storage ra:settings call set from storage ra:settings actions[$(a)]

# Whether this action navigates is read BEFORE it runs. The action itself writes
# to this storage -- a page show overwrites `call` on its way through -- so asking
# afterwards would be asking about whatever replaced it.
scoreboard players set #nav ra.set.tmp 0
execute if data storage ra:settings call{nav:1b} run scoreboard players set #nav ra.set.tmp 1

execute if data storage ra:settings call.f run function ra_settings:admin_call with storage ra:settings call
data remove storage ra:settings call

# Redraw, so the page shows what the click just did.
#
# Not after a navigation: Back already drew the index, and redrawing would put the
# page you just left straight back on top of it -- which is exactly why the menus
# appeared twice.
#
# Not while an input form is open either: the value has not been typed yet, and a
# redraw would bury the instructions.
execute if score #nav ra.set.tmp matches 0 unless score @s ra.settings.pend matches -1 run function ra_settings:admin_refresh
