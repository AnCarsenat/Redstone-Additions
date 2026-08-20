# /ra_settings:page/list
# Internal: one clickable line per page, numbered by position.

execute unless data storage ra:settings scan[0] run return 0

data modify storage ra:settings cur set from storage ra:settings scan[0]

# Page N is opened by the code N+4. 0 is undeliverable through /trigger; 1 is the
# player menu, 2 the server settings and 3 the disabled-blocks list.
scoreboard players operation #code ra.set.tmp = #idx ra.set.tmp
scoreboard players add #code ra.set.tmp 4
execute store result storage ra:settings cur.i int 1 run scoreboard players get #code ra.set.tmp
function ra_settings:page/list_row with storage ra:settings cur

scoreboard players add #idx ra.set.tmp 1
data remove storage ra:settings scan[0]
function ra_settings:page/list
