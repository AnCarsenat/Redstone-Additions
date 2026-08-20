# /ra_interactive:blocks/item_pipe/refresh_display
# Keep the little floating item that shows what this pipe is filtering for.
# Context: as the pipe marker, at the pipe position.
#
# The item frame used to be the display as well as the setting -- you could see
# what a pipe was sorting because the frame was right there on it. Moving the
# filter into a property would have made a sorting wall unreadable, so the pipe
# draws the item itself.
#
# Driven on the slow tick, because it is upkeep rather than logic: a filter
# changes when a player changes it, and a fifth of a second late is invisible.

data remove storage ra:temp fdisp
execute if data entity @s data.properties.filter_item run data modify storage ra:temp fdisp.id set from entity @s data.properties.filter_item

# No filter: no display.
execute unless data storage ra:temp fdisp.id run kill @e[type=item_display,tag=ra.pipe_filter,distance=..0.6]
execute unless data storage ra:temp fdisp.id run return 0

# Already showing the right thing: leave it alone rather than rebuilding it, or
# the item would flicker every time this ran.
execute as @e[type=item_display,tag=ra.pipe_filter,distance=..0.6] store success score #pf.same ra.temp run data modify storage ra:temp fdisp.id set from entity @s item.id
execute if entity @e[type=item_display,tag=ra.pipe_filter,distance=..0.6] if score #pf.same ra.temp matches 0 run return 0

kill @e[type=item_display,tag=ra.pipe_filter,distance=..0.6]
data modify storage ra:temp fdisp.count set value 1
function ra_interactive:blocks/item_pipe/spawn_display with storage ra:temp fdisp
