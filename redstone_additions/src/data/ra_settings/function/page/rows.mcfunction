# /ra_settings:page/rows
# Internal: draw each row of the current page, numbered by position.
#
# The number a button carries is the row's index, so the dispatcher looks the
# same list up again rather than keeping any state of its own -- the same trick
# the wrench menu uses, and for the same reason: state kept between a click and
# its handler is state that can go stale when a module reloads.

execute unless data storage ra:settings rowscan[0] run return 0

data modify storage ra:settings cur set from storage ra:settings rowscan[0]
# Codes are the row index PLUS ONE. /trigger cannot deliver 0 -- a trigger score
# of 0 is exactly the "not clicked" state the dispatcher waits on -- so row 0
# would be a button that silently did nothing.
scoreboard players operation #code ra.set.tmp = #idx ra.set.tmp
scoreboard players add #code ra.set.tmp 1
execute store result storage ra:settings cur.i int 1 run scoreboard players get #code ra.set.tmp

# A row with two buttons needs two codes. The second is the first plus 512, well
# past any real row count, so the dispatcher can split them apart with one
# comparison instead of carrying a separate "which button" score.
scoreboard players operation #up ra.set.tmp = #code ra.set.tmp
scoreboard players add #up ra.set.tmp 512
execute store result storage ra:settings cur.ip int 1 run scoreboard players get #up ra.set.tmp

# And a third for "Modify", which opens a typed input rather than stepping.
scoreboard players operation #ed ra.set.tmp = #code ra.set.tmp
scoreboard players add #ed ra.set.tmp 1024
execute store result storage ra:settings cur.ie int 1 run scoreboard players get #ed ra.set.tmp

# A user row's score may not exist yet. Seeding it at draw time means the value
# shown is the value that applies, instead of an empty bracket for everyone who
# has never touched the setting.
execute if data storage ra:settings cur{scope:"user"} run function ra_settings:row/seed_user with storage ra:settings cur

execute if data storage ra:settings cur{type:"bool"} run function ra_settings:row/bool with storage ra:settings cur
execute if data storage ra:settings cur{type:"int"} run function ra_settings:row/int with storage ra:settings cur
execute if data storage ra:settings cur{type:"prop"} run function ra_settings:row/prop with storage ra:settings cur
execute if data storage ra:settings cur{type:"str"} run function ra_settings:row/str with storage ra:settings cur
execute if data storage ra:settings cur{type:"list"} run function ra_settings:row/list with storage ra:settings cur
execute if data storage ra:settings cur{type:"block"} run function ra_settings:row/block with storage ra:settings cur

scoreboard players add #idx ra.set.tmp 1
data remove storage ra:settings rowscan[0]
function ra_settings:page/rows
