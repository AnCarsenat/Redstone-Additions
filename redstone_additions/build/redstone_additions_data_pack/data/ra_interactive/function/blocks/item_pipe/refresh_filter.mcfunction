# /ra_interactive:blocks/item_pipe/refresh_filter
# Re-read the item frame attached to this pipe and cache it on the marker.
# Context: as the pipe marker, at the pipe position.

scoreboard players set @s ra.filter_cd 20

data remove entity @s data.data.filter
function ra_interactive:blocks/item_pipe/find_filter
execute if data storage ra:temp filter_item.id run data modify entity @s data.data.filter set from storage ra:temp filter_item
