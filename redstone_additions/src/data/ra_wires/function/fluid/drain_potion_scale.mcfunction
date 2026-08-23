# /ra_wires:fluid/drain_potion_scale
# Internal: scale one effect's duration by how much was actually drawn, and apply it.
#
#   ticks = duration * drawn / 1000
#
# 1000 mL is one bottle, and the table's durations are what one bottle gives. The
# multiply happens before the divide so that a small draw does not round to
# nothing on the way through -- 100 mL of a 3600 tick effect is 360 ticks, not
# 3600 * (100/1000 = 0).
#
# Instant effects are stored with a duration of one tick, so they survive any
# scaling as at least one tick and behave the way drinking one does.

execute store result score #pt.dur ra.wires.tmp run data get storage ra:wires one.dur
execute if score #pt.dur ra.wires.tmp matches ..0 run return 0

scoreboard players operation #pt.dur ra.wires.tmp *= #pt.vol ra.wires.tmp
scoreboard players set #pt.bottle ra.wires.tmp 1000
scoreboard players operation #pt.dur ra.wires.tmp /= #pt.bottle ra.wires.tmp

# A draw too small to earn a whole tick still earns one: the player paid volume
# for it, and an effect of zero ticks is a command error rather than a no-op.
execute if score #pt.dur ra.wires.tmp matches ..0 run scoreboard players set #pt.dur ra.wires.tmp 1

# /effect give takes seconds, not ticks, and rounds down -- so an effect worth
# less than a second would vanish. Ticks are what the table and the scaling work
# in, so the conversion is done here and floored to one second.
scoreboard players set #pt.tps ra.wires.tmp 20
scoreboard players operation #pt.secs ra.wires.tmp = #pt.dur ra.wires.tmp
scoreboard players operation #pt.secs ra.wires.tmp /= #pt.tps ra.wires.tmp
execute if score #pt.secs ra.wires.tmp matches ..0 run scoreboard players set #pt.secs ra.wires.tmp 1

execute store result storage ra:wires one.secs int 1 run scoreboard players get #pt.secs ra.wires.tmp
function ra_wires:fluid/drain_potion_give with storage ra:wires one
